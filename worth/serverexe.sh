#!/bin/bash

# compila tutto
javac -cp .:./gson-2.8.6.jar *.java

java -cp .:./gson-2.8.6.jar MainServer
