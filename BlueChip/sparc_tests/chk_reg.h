#define check(reg,val)	\
	sethi	%hi(val),ERR_RET; \
	or	ERR_RET,%lo(val),ERR_RET; \
	cmp	reg,ERR_RET; \
	check_error(NEXTE,bne)
	
