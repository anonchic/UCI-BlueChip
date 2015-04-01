EXE = simulator to-little-e
all: $(EXE)

CC = gcc
CFLAGS = -g -W -Wall

SIM_OBJS = simulator.o main.o priv.o
LITTLE_E_OBJS = to-little-e.o

OBJS = $(SIM_OBJS) $(LITTLE_E_OBJS)

-include $(OBJS:.o=.d)

simulator: $(SIM_OBJS)
	$(CC) $(CFLAGS) -o $@ $(SIM_OBJS)

to-little-e: $(LITTLE_E_OBJS)
	$(CC) $(CFLAGS) -o $@ $(LITTLE_E_OBJS)

%.d: %.c
	set -e; $(CC) -MM $(CFLAGS) $< \
		| sed 's/\($*\)\.o[ :]*/\1.o $@ : /g' > $@; \
		[ -s $@ ] || rm -f $@

%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

clean: 
	rm -f $(EXE) *.o *~ core.* *.d
