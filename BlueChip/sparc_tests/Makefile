
XCC=sparc-linux-gcc $(BOPT)
XAS=sparc-linux-gcc -c -I. $(BOPT)
XAR=sparc-elf-ar
XCFLAGS=-mv8
XLDFLAGS=-L$(VPATH) $(VPATH)/lib3tests.a


VPATH = .

PROGS := $(shell ls $(VPATH)/ | grep st_ | grep .S | grep --invert-match \~ | grep --invert-match \\\#)
PROGS := $(PROGS:.S=)

OFILES = $(PROGS:%=$(VPATH)/%.o)
IMGFILES = $(PROGS:%=$(VPATH)/%.img)
ASMFILES = $(PROGS:%=$(VPATH)/%.asm)

all: $(IMGFILES)

asm: $(ASMFILES)


#st_arithmetic.o: st_arithmetic.S
#	sparc-linux-gcc -c -mv8  st_arithmetic.S -o st_arithmetic.o

%.tmpbin: %.o
	sparc-linux-ld -s -Ttext 0 --oformat binary -o $@ $<

%.bin: %.tmpbin
	../to-little-e < $< > $@

%.img: %.bin
	dd if=/dev/zero of=$@ bs=1024 count=1024
	dd if=$< of=$@ conv=notrunc bs=1024

%.asm: %.o
	sparc-linux-objdump -d $< > $@

%.o: %.S
	$(XCC) $(XCFLAGS) -c $< -o $@

%.o: %.c
	$(XCC) $(XCFLAGS) -c $< -o $@

clean: 
	rm -f *.o *~ core.* *.d *.img *.tmpbin *.bin *.asm