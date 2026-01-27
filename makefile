#Author: Daltro Oliveira Vinuto Email: daltroov777@gmail.com

all: compile generate_cap

JAVA8_BIN :=/usr/lib/jvm/java-8-openjdk-amd64/bin

JAVA_SRC :=card/AuthApplet

CONTROLLER_SRC :=./py/fase2_auth

compile:
	$(JAVA8_BIN)/javac -version
	@echo 

	$(JAVA8_BIN)/javac -source 1.2 -target 1.2 \
	-cp ~/javacard-sdk-3.0.5/lib/api_classic.jar \
	-d bin \
	src/$(JAVA_SRC).java

	@echo "Compilation finished, file *.class created\n"

JAVACARD_SDK :=/home/daltro/java_card_kit-2_2_2-rr-bin-linux-do

generate_cap: 
	$(JAVA8_BIN)/java -cp \
	$(JAVACARD_SDK)/lib/converter.jar:$(JAVACARD_SDK)/lib/api.jar \
	com.sun.javacard.converter.Converter \
	-classdir bin \
	-exportpath $(JAVACARD_SDK)/api_export_files \
	-applet 0xA0:0x00:0x00:0x00:0x62:0x03:0x01:0x0C:0x01 $(JAVA_SRC) \
	card 0xA0:0x00:0x00:0x00:0x62:0x03:0x01:0x0C 1.0 \
	-noverify

	@echo "*.cap file created with success\n"
	 

JAVA11_BIN :=/opt/java/jdk-11/bin
GP := ~/gp_v25.10.20/gp.jar
RUN_GP :=$(JAVA11_BIN)/java -jar  $(GP)

load_cap:
	$(RUN_GP) -install bin/card/javacard/card.cap


CAP_INSTANCE := A00000006203010C01
CAP_PACKAGE := A00000006203010C
delete_cap:
	rm -rf bin/*
	$(RUN_GP) --delete $(CAP_INSTANCE)
	$(RUN_GP) --delete $(CAP_PACKAGE)


APDU_1 := 00A4040009A00000006203010C01
APDU_2 := 80010000
send_apdu:
	$(RUN_GP) -a $(APDU_1) -a $(APDU_2)


list:
	$(RUN_GP) --list

info:
	$(RUN_GP) --info


test:
	python3  $(CONTROLLER_SRC).py
