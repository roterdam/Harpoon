/* generic_math_int64.h - Native methods for 64bit math operations
   Copyright (C) 1998 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.
 
GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

/*
Description: generic target global defintions
Systems    : all
*/

#ifndef __TARGET_GENERIC__
#define __TARGET_GENERIC__

#ifdef __cplusplus
extern "C" {
#endif

/* check if target_native_network.h included */
#ifndef __TARGET_NATIVE__
  #error Do NOT INCLUDE generic target files! Include the corresponding native target files instead!
#endif

/****************************** Includes *******************************/
/* do not move; needed here because of some macro definitions */
#include "config.h"

#include <stdlib.h>
#include <errno.h>

/****************** Conditional compilation switches *******************/

/***************************** Constants *******************************/
#define TARGET_NATIVE_OK    1
#define TARGET_NATIVE_ERROR 0

#define TARGET_NATIVE_ERROR_PERMISION_DENIED        EACCES
#define TARGET_NATIVE_ERROR_BAD_FILE_DESCRIPTOR     EBADF
#define TARGET_NATIVE_ERROR_FILE_EXISTS             EEXIST
#define TARGET_NATIVE_ERROR_INPUT_OUTPUT            EIO
#define TARGET_NATIVE_ERROR_TOO_MANY_OPEN_FILES     EMFILE
#define TARGET_NATIVE_ERROR_FILENAME_TO_LONG        ENAMETOOLONG
#define TARGET_NATIVE_ERROR_NO_SUCH_DEVICE          ENODEV
#define TARGET_NATIVE_ERROR_NO_SUCH_FILE            ENOENT
#define TARGET_NATIVE_ERROR_NO_SPACE_LEFT           ENOSPC
#define TARGET_NATIVE_ERROR_DIRECTORY_NOT_EMPTY     ENOTEMPTY
#define TARGET_NATIVE_ERROR_OPERATION_NOT_PERMITTED EPERM
#define TARGET_NATIVE_ERROR_READ_ONLY_FILE_SYSTEM   EROFS
#define TARGET_NATIVE_ERROR_INVALID_SEEK            ESPIPE
#define TARGET_NATIVE_ERROR_INTERRUPT_FUNCTION_CALL EINTR

/***************************** Datatypes *******************************/

/***************************** Variables *******************************/

/****************************** Macros *********************************/

/***********************************************************************\
* Name       : TARGET_NATIVE_LAST_ERROR
* Purpose    : return last error code
* Input      : -
* Output     : -
* Return     : error code
* Side-effect: unknown
* Notes      : -
\***********************************************************************/

#ifndef TARGET_NATIVE_LAST_ERROR
  #include <errno.h>
  #define TARGET_NATIVE_LAST_ERROR() \
    errno
#endif

/***********************************************************************\
* Name       : TARGET_NATIVE_LAST_ERROR_STRING
* Purpose    : return last error string
* Input      : -
* Output     : -
* Return     : error string (read only!)
* Side-effect: unknown
* Notes      : -
\***********************************************************************/

#ifndef TARGET_NATIVE_LAST_ERROR_STRING
  #include <string.h>
  #include <errno.h>
  #define TARGET_NATIVE_LAST_ERROR_STRING() \
    strerror(errno)
#endif

/***************************** Functions *******************************/

#ifdef __cplusplus
}
#endif

#endif /* __TARGET_GENERIC__ */

/* end of file */
