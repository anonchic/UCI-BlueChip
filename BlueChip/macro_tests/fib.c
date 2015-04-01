#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>

#define N 34

void start(int *printPtr) {
    int i;
    for(i = 0; i != N; i++) {
        *printPtr = fib(i);
    }
}

int fib(int n) {
    if(n == 0) {
        return 0;
    }
    
    if(n == 1) {
        return 1;
    }

    return fib(n-1) + fib(n-2);
}

/*
int main(void) {
    int idx;
    for(idx = 0; idx < N; idx++) {
        printf("%d\n", fib(idx));
    }
    return 0;
}
*/
