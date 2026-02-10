#Author: Daltro Oliveira Vinuto Email: daltroov777@gmail.com

all: cls compile create_cap 

USER_NAME = $(USER)
JAVA_CLASS := HelloWorld#AuthApplet
SRC := src/card
SRC_FILE := $(SRC)/$(JAVA_CLASS)
JAVA_SRC_FILE := $(SRC_FILE).java
TARGET_FOLDER := bin
CARD := card
JAVACARD := javacard
CONTROLLER_SRC :=./py/fase2_auth

APDU_1 := 00A4040009A00000006203010C01
APDU_2 := 80010000

CAP_INSTANCE := A00000006203010C01
CAP_PACKAGE := A00000006203010C

JAVA11_BIN :=/opt/java/jdk-11/bin
JAVA8_BIN :=/usr/lib/jvm/java-8-openjdk-amd64/bin

JAVACARD_SDK_2_2 :=/home/$(USER_NAME)/System_Software/java_card_kit-2_2_2-rr-bin-linux-do
JAVACARD_SDK_3 := /home/$(USER_NAME)/System_Software/javacard-sdk-3.0.5

GP := /home/$(USER_NAME)/System_Software/gp_v25.10.20/gp.jar
RUN_GP :=$(JAVA11_BIN)/java -jar  $(GP)
GP_FLAGS :=-d#-d

PYTHON3 := python3

check:
	mkdir -p src

cls:
	clear

compile:
	@echo "============================> Compilation started\n"
	$(JAVA8_BIN)/javac -version
	@echo 
	$(JAVA8_BIN)/javac -source 1.2 -target 1.2 \
	-cp $(JAVACARD_SDK_3)/lib/api_classic.jar \
	-d $(TARGET_FOLDER) \
	$(JAVA_SRC_FILE)
	@echo "\n=============================> Compilation finished, file *.class created\n"

create_cap: 
	@echo "============================> cap creation started\n"
	$(JAVA8_BIN)/java -cp \
	$(JAVACARD_SDK_2_2)/lib/converter.jar:$(JAVACARD_SDK_2_2)/lib/api.jar \
	com.sun.$(JAVACARD).converter.Converter \
	-classdir $(TARGET_FOLDER) \
	-exportpath $(JAVACARD_SDK_2_2)/api_export_files \
	-applet 0xA0:0x00:0x00:0x00:0x62:0x03:0x01:0x0C:0x01 $(CARD)/$(JAVA_CLASS) \
	$(CARD) 0xA0:0x00:0x00:0x00:0x62:0x03:0x01:0x0C 1.0 \
	-noverify
	@echo "\n============================> cap creation finished\n"

load_cap:
	@echo "============================> loading cap started\n"
	$(RUN_GP) -install $(TARGET_FOLDER)/$(CARD)/$(JAVACARD)/$(CARD).cap  $(GP_FLAGS)
	@echo "\n============================> cap loaded\n"

delete_cap:
	@echo "============================> deleting *.class, *.cap, Applet from the Javacard\n"

	rm -rf bin/*
	$(RUN_GP) --delete $(CAP_INSTANCE)
	$(RUN_GP) --delete $(CAP_PACKAGE)

	@echo "\n============================> files *.class, *.cap and Applet deleted\n"


send_apdu:
	@echo "============================> sending APDU to the JavaCard\n"

	$(RUN_GP) -a $(APDU_1) -a $(APDU_2)  $(GP_FLAGS)

	@echo "\n============================> JavaCard answer is displayed above\n"


list:
	@echo "============================> listing JavaCard applets\n"

	$(RUN_GP) --list $(GP_FLAGS)

	@echo "\n============================> JavaCard applets listed\n"


info:
	@echo "============================> getting information about the JavaCard\n"

	$(RUN_GP) --info $(GP_FLAGS)

	@echo "\n============================> information about JavaCard is displayed above\n"

test:
	$(PYTHON3)  $(CONTROLLER_SRC).py

secure_hello:
	@echo "Establishing Secure Channel and Sending Hello APDU...\n"
	$(JAVA11_BIN)/java -jar $(GP) \
		-s 00A4040009A00000006203010C01 \
		-s 80010000 \
		$(GP_FLAGS)
	@echo "\n============================> JavaCard answer decrypted below\n"

