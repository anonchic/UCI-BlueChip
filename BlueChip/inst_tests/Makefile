all: alu.img

CC = gcc
CFLAGS = -g -W -Wall
LDFLAGS =

alu.o: alu.S
	sparc-linux-gcc -c -mv8 -mflat alu.S -o alu.o

alu.tmp.bin: alu.o
	sparc-linux-ld -s -Ttext 0 --oformat binary -o alu.tmp.bin alu.o

alu.bin: alu.tmp.bin
	../to-little-e < alu.tmp.bin > alu.bin

alu.img: alu.bin
	dd if=/dev/zero of=alu.img bs=1024 count=1024
	dd if=alu.bin of=alu.img conv=notrunc bs=1024

alu.asm: alu.o
	sparc-linux-objdump -d alu.o > alu.asm

%.o: %.s
	gcc -o $@ -c $<

%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

clean: 
	rm -f *.o *~ core.* *.d alu.img alu.o alu.tmp.bin alu.bin alu.img alu.asm

