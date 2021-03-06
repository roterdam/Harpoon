#ifndef INCLUDED_FREE_LIST_H
#define INCLUDED_FREE_LIST_H

#include "jni-gc.h"
#include "jni-types.h"
#include "jni-private.h"

/* data structure for a block of memory */
struct block
{
#ifdef WITH_PRECISE_GC_STATISTICS
  jint time;
#endif /* WITH_PRECISE_GC_STATISTICS */
  size_t size;
  union { ptroff_t mark; struct block *next; } markunion;
  struct oobj object[0];
};

#include "free_list_consts.h"

#define UNREACHABLE 1
#define REACHABLE   2
#define MARK_OFFSET 3

#define CLEAR_MARK(bl) ({ (bl)->markunion.mark = UNREACHABLE; })
#define MARK_AS_REACHABLE(bl) ({ (bl)->markunion.mark = REACHABLE; })

#define MARKED_AS_REACHABLE(bl) ((bl)->markunion.mark == REACHABLE)
#define NOT_MARKED(bl) ((bl)->markunion.mark == UNREACHABLE)

#define GET_INDEX(bl) (bl->markunion.mark - MARK_OFFSET);
#define SET_INDEX(bl,index) ({ (bl)->markunion.mark = (index) + MARK_OFFSET; })

#ifdef DEBUG_GC
void debug_clear_memory(struct block *bl);
#else /* DEBUG_GC */
# define debug_clear_memory(bl) ((void)0)
#endif /* !DEBUG_GC */

/* effects: adds small blocks to the small blocks table, and
   large blocks to the correct location in the free list
*/
void add_to_free_blocks(struct block *new_block,
			struct block **free_list,
			struct block *small_blocks[]);

/* returns: a block that is greater than or equal in size to the
   request; block is initialized before being returned
 */
struct block *find_free_block(size_t size,
			      struct block **free_list_ptr,
			      struct block *small_blocks[]);

#endif /* INCLUDED_FREE_LIST_H */
