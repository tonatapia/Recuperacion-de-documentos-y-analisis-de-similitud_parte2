#!/usr/bin/env python3
import sys

for linea in sys.stdin:
    linea = linea.strip()

    if linea == "":
        continue

    partes = linea.split("\t")

    if len(partes) != 3:
        continue

    documento = partes[0]
    palabra = partes[1]
    frecuencia = partes[2]

    print(documento + "\t" + palabra + "\t" + frecuencia)