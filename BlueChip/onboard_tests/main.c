#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>

#include <assert.h>

#define N 10

int fib(int n) {
    if(n == 0) {
        return 0;
    }

    if(n == 1) {
        return 1;
    }

    return fib(n-1) + fib(n-2);
}

int main(int argc, char *argv[]) {
    int results[N];
    int idx, ret, fd;
    char buf[10];

    fd = open("/proc/hw_watch", O_RDWR);
    assert(fd >= 0);
    sprintf(buf, "%08x", (int) fib);
    printf("writing %s\n", buf);
    ret = write(fd, buf, 9);
    for(idx = 0; idx != N; idx++) {
        results[idx] = fib(idx);
        printf("%d\n", results[idx]);
    }    

    return 0;
}
