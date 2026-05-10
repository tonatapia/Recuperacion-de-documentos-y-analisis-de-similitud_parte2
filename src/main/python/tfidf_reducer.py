#!/usr/bin/env python3
import sys

documento_actual = None
palabras = []
total_palabras = 0

def calcular_tf(documento, palabras, total):
    for palabra, frecuencia in palabras:
        tf = frecuencia / total
        print(palabra + "\t" + documento + ":" + str(tf))

for linea in sys.stdin:
    linea = linea.strip()

    if linea == "":
        continue

    partes = linea.split("\t")

    if len(partes) != 3:
        continue

    documento = partes[0]
    palabra = partes[1]
    frecuencia = int(partes[2])

    if documento_actual is None:
        documento_actual = documento

    if documento != documento_actual:
        calcular_tf(documento_actual, palabras, total_palabras)

        documento_actual = documento
        palabras = []
        total_palabras = 0

    palabras.append((palabra, frecuencia))
    total_palabras += frecuencia

if documento_actual is not None:
    calcular_tf(documento_actual, palabras, total_palabras)