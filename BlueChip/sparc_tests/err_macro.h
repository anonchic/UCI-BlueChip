#define NEXTE __LINE__

#define check_error(num, branch)	\
    branch##,a testretg1;		\
    set num##,%g1;			\
    nop;				\

#define check_error_reg(reg, branch)	\
    branch##,a testretg1;		\
    mov reg##,%g1;			\
    nop;				\

#define new_init(testname) \
    .text;		   \
    .align 4;			      \
    .global _start; \
_start:    b _st_##testname;				\
    .global _st_##testname##, st_##testname##;		\
    .proc	04 ;				\
_st_##testname:##;					\
st_##testname##:;					\
.text;						\
b savestate ;					\
nop ;

#define murph_init(testname) \
    .text;		   \
    .align 4;			      \
    .global _murph_##testname##, murph_##testname##;		\
    .proc	04 ;				\
_murph_##testname:##;					\
murph_##testname##:;					\
.text;						\
b savestate ;					\
nop ;
