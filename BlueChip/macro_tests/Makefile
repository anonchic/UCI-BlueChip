all: fib.o fib.s fib.bin fib.img fib.asm

CC = gcc
CFLAGS = -g -W -Wall
LDFLAGS =

fib.asm: fib.o
	sparc-linux-objdump -d fib.o > fib.asm

fib.img: fib.bin
	dd if=/dev/zero of=fib.img bs=1024 count=1024
	dd if=fib.bin of=fib.img conv=notrunc bs=1024

fib.bin: tmp.bin
	../to-little-e < tmp.bin > fib.bin

tmp.bin: fib.o
	sparc-linux-ld -s -Ttext 0 --oformat binary -o tmp.bin fib.o

fib.o: fib.s
	sparc-linux-gcc -c -mv8 fib.s -o fib.o

fib.s: fib.c
	sparc-linux-gcc -S -mv8 fib.c -o fib.s

to-little-e: to-little-e.c
	gcc -o to-little-e -W -Wall -g to-little-e.c

%.o: %.s
	gcc -o $@ -c $<

%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

clean: 
	rm -f *.o *~ core.* *.d fib to-little-e fib.bin tmp.bin fib.img fib.s fib.asm
