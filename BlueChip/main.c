#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <signal.h>
#include <time.h>
#include <string.h>

#include "simulator.h"

#define RAM_SIZE (1024*1024)
static unsigned char *ram = NULL;

void unknownInst(struct CPU *cpu, struct control_signals *control) {
    printf("op = 0x%x\n", control->op);
    printf("op2 = 0x%x\n", control->op2);
    printf("op3 = 0x%x\n", control->op3);
    printf("pc = 0x%x\n", cpu->pc);
    assert(false);
}


bool guestLoad(u32 guestAddr, u32 *value) {
    assert((guestAddr & 0x3) == 0);

    if(guestAddr > (RAM_SIZE-4)) {
        return false;
    }
    *value = *((u32 *)(ram + guestAddr));
    return true;
}

bool guestStore(u32 guestAddr, u32 value) {
    assert((guestAddr & 0x3) == 0);

    if(guestAddr > (RAM_SIZE-4)) {
        return false;
    }
    *((u32 *) (ram+guestAddr)) = value;
    if(guestAddr == (RAM_SIZE - 4)) {
        printf("%u\n", value);
    }
    
    /* Special address for tests */
    if(guestAddr == (RAM_SIZE - 8)) {
	if (value == 0) {
	    printf("Test Completed Sucessfully!\n");
	} else {
	    printf("Test Failed on line %u\n", value);
	}
	exit(-value);
    }

    return true;
}

static void cpu_exec(struct CPU *cpu) {
    bool runSimulation = true;
    while(runSimulation) {
        runSimulation = cpu_exec_inst(cpu);
    }
}

static void fillState(char *fileName, void *buf, int size) {
    int ret, fd;

    assert(size > 0);

    fd = open(fileName, O_RDONLY);
    assert(fd >= 0);

    ret = read(fd, buf, size);
    assert((ret <= size) && (ret>0));

    close(fd);
}


int main(int argc, char *argv[]) {
    struct CPU *cpu;
    int idx;

    if(argc != 2) {
        fprintf(stderr, "Usage: %s memory_file\n", argv[0]);
        return -1;
    }

    // initialize our state
    ram = malloc(RAM_SIZE);
    cpu = malloc(sizeof(struct CPU));
    for(idx = 0; idx < NUM_REGS; idx++) {
        cpu->regs[idx] = 0;
    }
    cpu->pc = 0;
    cpu->npc = 4;
    cpu->psr = 0;

    /* Set Supervisor bit */
    cpu->psr |= (1 << 7);

    cpu->wim = 1<<1;
    cpu->branch_pc = (u32) -1;
    // setup the stack pointer
    cpu->regs[SP] = RAM_SIZE-4-120;

    // setup the fib program
    cpu->regs[8] = RAM_SIZE-4;

    // fetch our memory image and cpu state (if set)
    fillState(argv[1], ram, RAM_SIZE);
    if(argc >= 3) {        
        fillState(argv[2], cpu, sizeof(struct CPU));
    }
    
    cpu_exec(cpu);

    return 0;
}
