

orbit.o: orbit.c
	gcc -c -g -o orbit.o orbit.c

orbitr.o: orbitr.c
	gcc -c -g -o orbitr.o orbitr.c

orbit:: orbit.o orbitr.o
	gcc -o orbit orbit.o orbitr.o -lm

nasa:: nasa.o
	gcc -lm  nasa.o -o nasa


