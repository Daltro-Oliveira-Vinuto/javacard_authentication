import subprocess
import os
import sys
import re
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives import hashes

USER_NAME = os.getenv("USER")


# --- CONFIGURAÇÃO ---
GP_JAR = os.path.expanduser(f"/home/{USER_NAME}/System_Software/gp_v25.10.20/gp.jar") 
#print(f"{GP_JAR}")

AID = "A00000006203010C01"
JAVA11_BIN = "/opt/java/jdk-11/bin/java"

# Caminhos das chaves OpenSSL
PRIV_KEY_FILE = "ecc/keys/ecc_private.pem"
PUB_KEY_FILE  = "ecc/keys/ecc_public.pem"

print(f"Private key file path: {PRIV_KEY_FILE}")
print(f"Public key file path: {PUB_KEY_FILE}")


def run_gp(apdus):
    args = " ".join([f"-a {x}" for x in apdus])
    cmd = f"{JAVA11_BIN} -jar {GP_JAR} {args} -d"
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return result.stdout + result.stderr

def check_transaction_status(output):
    clean = output.replace(" ", "").replace("\n", "")
    if "6985" in clean: return "LOCKED"
    elif "9000" in clean: return "SUCCESS"
    else: return "ERROR"

def extract_signature(output):
    """
    Extrai a assinatura robustamente, ignorando metadados do GP como '(62ms)'
    """
    # Procura por linhas que contenham 'A<<' e '9000'
    for line in output.splitlines():
        if "A<<" in line and "9000" in line:
            # Exemplo de linha: "A<< (0071+2) (62ms) 304502... 9000"
            
            # 1. Removemos o status '9000' do final
            line_no_status = line.split("9000")[0]
            
            # 2. Dividimos por espaços para pegar os pedaços
            parts = line_no_status.split()
            
            # 3. Pegamos o ÚLTIMO pedaço, que deve ser a assinatura hexadecimal
            # (Ignora 'A<<', '(62ms)', etc)
            candidate_hex = parts[-1].strip()
            
            # 4. Verifica se parece uma assinatura ECDSA (começa com 30 e é longa)
            if candidate_hex.startswith("30") and len(candidate_hex) > 64:
                try:
                    return bytes.fromhex(candidate_hex)
                except ValueError:
                    continue
    return None

def load_openssl_private_hex():
    try:
        with open(PRIV_KEY_FILE, "rb") as f:
            private_key = serialization.load_pem_private_key(f.read(), password=None)
            private_val = private_key.private_numbers().private_value
            return private_val.to_bytes(32, 'big').hex().upper()
    except FileNotFoundError:
        print(f"❌ ERRO: Arquivo '{PRIV_KEY_FILE}' não encontrado.")
        sys.exit(1)

def load_openssl_public_key():
    try:
        with open(PUB_KEY_FILE, "rb") as f:
            return serialization.load_pem_public_key(f.read())
    except FileNotFoundError:
        print(f"❌ ERRO: Arquivo '{PUB_KEY_FILE}' não encontrado.")
        sys.exit(1)

# --- MENU ---
print("\n--- AUTENTICAÇÃO VIA OPENSSL KEYS ---")
print(f"Chaves: {PRIV_KEY_FILE} e {PUB_KEY_FILE}")
print("1. PROVISIONAR CARTÃO (Gravar Chave)")
print("2. VERIFICAR CARTÃO (Assinar Desafio)")
opcao = input("Escolha: ").strip()

if opcao == "1":
    print("\n[1] Lendo chave privada do arquivo OpenSSL...")
    priv_hex = load_openssl_private_hex()
    
    print(f"[2] Gravando no cartão...")
    apdus = [f"00A4040009{AID}", f"8010000020{priv_hex}"]
    
    out = run_gp(apdus)
    status = check_transaction_status(out)
    
    if status == "SUCCESS":
        print("✅ SUCESSO: Chave gravada e parâmetros de curva configurados.")
    elif status == "LOCKED":
        print("🛡️ BLOQUEADO: O cartão já possui uma chave.")
        print("   (Isso é bom! O Write-Once está funcionando)")
    else:
        print("❌ ERRO NO GP:")
        print(out)

elif opcao == "2":
    print("\n[1] Carregando chave pública...")
    pub_key = load_openssl_public_key()
    
    print("[2] Enviando desafio...")
    challenge = os.urandom(16)
    print(f"    Desafio: {challenge.hex()}")

    cmd_sign = f"8020000010{challenge.hex()}"
    out = run_gp([f"00A4040009{AID}", cmd_sign])
    
    signature = extract_signature(out)

    if signature:
        print(f"\n[3] Assinatura recebida ({len(signature)} bytes). Validando matemática...")
        try:
            pub_key.verify(signature, challenge, ec.ECDSA(hashes.SHA256()))
            print("✅ SUCESSO TOTAL: Assinatura Válida!")
            print("   O cartão ASSINOU CORRETAMENTE usando a chave privada oculta.")
        except Exception as e:
            print("❌ FALHA: Assinatura Matemática Inválida.")
            print(f"   Erro detalhado: {e}")
    else:
        print("❌ ERRO: Não consegui extrair a assinatura do log.")
        print("--- LOG BRUTO ---")
        print(out)