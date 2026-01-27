import subprocess
import os

# --- CONFIGURAÇÃO ---
GP_JAR = os.path.expanduser("~/gp_v25.10.20/gp.jar") 
AID = "A00000006203010C01"
# Uma chave privada de teste (32 bytes - ECC P-256)
TEST_KEY = "11223344556677889900AABBCCDDEEFF11223344556677889900AABBCCDDEEFF"
JAVA11_BIN ="/opt/java/jdk-11/bin"


def send_apdu(apdu):
    cmd = f"{JAVA11_BIN}/java -jar {GP_JAR} -a {apdu} -d"
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return result.stdout + result.stderr

print("--- INICIANDO FASE 1: TESTE DE ARMAZENAMENTO ---")

# 1. Selecionar o Applet
select_apdu = f"00A4040009{AID}"
print(f"[1] Selecionando Applet...")

opcao_escolhida = int(input("Digite a opcao(1 para enviar chave e 2 para solicitar chave): "))


if opcao_escolhida == 1: 
    # 2. Enviar a Chave (INS 0x10)
    # Header: 80 10 00 00 20 (onde 20 hex = 32 bytes) + DADOS
    store_apdu = f"8010000020{TEST_KEY}"
    print(f"[2] Enviando chave para a EEPROM: {TEST_KEY}")
    subprocess.run(f"{JAVA11_BIN}/java -jar {GP_JAR} -a {select_apdu} -a {store_apdu}", shell=True)

elif opcao_escolhida == 2:
    # 3. Ler a Chave de Volta (INS 0x20)
    retrieve_apdu = "8020000020" # Solicita 32 bytes de volta
    print(f"[3] Solicitando leitura da memória do cartão...")
    output = send_apdu(f"{select_apdu} -a {retrieve_apdu}")

    print(f"O output do javacard foi: {output}")

    # 4. Verificação Visual
    print("\n--- RESULTADO ---")
    if TEST_KEY.lower() in output.replace(" ", "").lower():
        print("SUCESSO: A chave enviada é IDÊNTICA à recebida.")
        print("O armazenamento persistente está funcionando!")
    else:
        print("ERRO: O cartão devolveu dados diferentes ou falhou.")
        print("Raw Output:", output)

