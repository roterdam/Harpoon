/* Macros to access the load linked and store conditional instructions
 * on this architecture.
 *  C. Scott Ananian <cananian@alumni.princeton.edu>
 */
#ifndef INCLUDED_LLSC_H
#define INCLUDED_LLSC_H

static inline int32_t load_linked(int32_t *ptr) {
  uint32_t result;
  __asm__ volatile ("lwarx %0,0,%1" : "=r"(result) : "r"(ptr));
  return result;
}
static inline int store_conditional(int32_t *ptr, int32_t val) {
  int result;
  __asm__ ("\
	stwcx. %2,0,%1
	li %0,0
	bne- 0f
	addi %0,1
0:
" : "=r"(result) : "r"(ptr), "r"(val) : "cr0", "memory");
  return result;
}

static inline int64_t load_linked_double(int64_t *ptr) {
  uint64_t result;
  __asm__ volatile ("ldarx %0,0,%1" : "=r"(result) : "r"(ptr));
  return result;
}
static inline int store_conditional_double(int64_t *ptr, int64_t val) {
  int result;
  __asm__ ("\
	stdcx. %2,0,%1
	li %0,0
	bne- 0f
	addi %0,1
0:
" : "=r"(result) : "r"(ptr), "r"(val) : "cr0", "memory");
  return result;
}

static inline void sync(void) {
  __asm__ volatile ("sync" ::: "memory" );
}

#define LL(x) load_linked(x)
#define LL_D(x) load_linked_double(x)
#define SC(x,y) store_conditional(x,y)
#define SC_D(x,y) store_conditional_double(x,y)

#endif /* INCLUDED_LLSC_H */
