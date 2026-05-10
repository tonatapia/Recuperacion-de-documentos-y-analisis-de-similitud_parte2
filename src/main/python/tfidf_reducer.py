#!/usr/bin/env python3
import sys
import math

documentos = {}
vocabulario = set()

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

    if documento not in documentos:
        documentos[documento] = {}

    documentos[documento][palabra] = frecuencia
    vocabulario.add(palabra)

total_documentos = len(documentos)

for palabra in sorted(vocabulario):
    lista_documentos = []

    for documento in documentos:
        if palabra in documentos[documento]:
            lista_documentos.append(documento)

    documentos_con_palabra = len(lista_documentos)

    if documentos_con_palabra == 0:
        continue

    idf = math.log(total_documentos / documentos_con_palabra)

    resultados = []

    for documento in lista_documentos:
        total_palabras_doc = sum(documentos[documento].values())
        frecuencia = documentos[documento][palabra]

        tf = frecuencia / total_palabras_doc
        tfidf = tf * idf

        resultados.append(documento + ":TF=" + str(round(tf, 6)) + ":TFIDF=" + str(round(tfidf, 6)))

    print(palabra + "\tIDF=" + str(round(idf, 6)) + "\t" + ",".join(resultados))