
#include "SqliteDatabase.h"
#include <string.h>
#include "sqlite3.h"

inline void* toref(jlong value) { jvalue ret; ret.j = value; return (void *) ret.l; }
inline jlong fromref(void * value) { jvalue ret; ret.l = value; return ret.j; }


#define THROW_NULLPTREXCEPTION(env, msg)  \
	(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/NullPointerException"), msg);

#define THROW_IOEXCEPTION(env, msg)  \
	(*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"), msg);



JNIEXPORT jstring JNICALL Java_org_wwscc_storage_SqliteDatabase_libversion
  (JNIEnv *env, jclass cls)
{
	const char *ver = sqlite3_libversion();
	return (*env)->NewStringUTF(env, ver);
}


JNIEXPORT jlong JNICALL Java_org_wwscc_storage_SqliteDatabase_open
  (JNIEnv *env, jobject this, jstring filename, jboolean shared, jint timeout)
{
	const char *str;
	sqlite3 *db;

	sqlite3_enable_shared_cache(shared ? 1 : 0);

	str = (*env)->GetStringUTFChars(env, filename, 0); 
	if (sqlite3_open(str, &db)) 
	{
		sqlite3_close(db);
		THROW_IOEXCEPTION(env, sqlite3_errmsg(db));
		return 0;
	}
	(*env)->ReleaseStringUTFChars(env, filename, str);

	sqlite3_busy_timeout(db, timeout);
	return fromref(db);
}

JNIEXPORT void JNICALL Java_org_wwscc_storage_SqliteDatabase_close
	(JNIEnv *env, jobject this, jlong dbmarker)
{
	sqlite3* db = toref(dbmarker);
	sqlite3_stmt *pStmt;
	while((pStmt = sqlite3_next_stmt(db, 0))!=0 )
	{
		sqlite3_finalize(pStmt);
	}

	if (sqlite3_close(db))
	{
		THROW_IOEXCEPTION(env, sqlite3_errmsg(db));
	}
}

JNIEXPORT jlong JNICALL Java_org_wwscc_storage_SqliteDatabase_prepare
	(JNIEnv *env, jobject this, jlong dbmarker, jstring sql)
{
	sqlite3* db = toref(dbmarker);
	sqlite3_stmt* stmt = NULL;

	if (sql == NULL)
	{
		THROW_NULLPTREXCEPTION(env, "sql is null");
		return 0;
	}

	const char *strsql = (*env)->GetStringUTFChars(env, sql, 0);
	int status = sqlite3_prepare_v2(db, strsql, -1, &stmt, 0);
	(*env)->ReleaseStringUTFChars(env, sql, strsql);

	if (status != SQLITE_OK) 
	{
		THROW_IOEXCEPTION(env, sqlite3_errmsg(db));
		return 0;
	}

	return fromref(stmt);
}

JNIEXPORT jlong JNICALL Java_org_wwscc_storage_SqliteDatabase_lastInsertId
  (JNIEnv *env, jobject this, jlong dbmarker)
{
	return sqlite3_last_insert_rowid(toref(dbmarker));
}

JNIEXPORT jstring JNICALL Java_org_wwscc_storage_SqliteDatabase_errmsg
  (JNIEnv *env, jobject this, jlong dbmarker)
{
	return (*env)->NewStringUTF(env, sqlite3_errmsg(toref(dbmarker)));
}


JNIEXPORT jint JNICALL Java_org_wwscc_storage_SqliteDatabase_changes
  (JNIEnv *env, jobject this, jlong dbmarker)
{
	return sqlite3_changes(toref(dbmarker));
}

JNIEXPORT jint JNICALL Java_org_wwscc_storage_SqliteDatabase_finalize
  (JNIEnv *env, jobject this, jlong stmt)
{
	return sqlite3_finalize(toref(stmt));
}


JNIEXPORT jint JNICALL Java_org_wwscc_storage_SqliteDatabase_step
  (JNIEnv *env, jobject this, jlong stmt)
{
	return sqlite3_step(toref(stmt));
}


JNIEXPORT jint JNICALL Java_org_wwscc_storage_SqliteDatabase_reset
  (JNIEnv *env, jobject this, jlong stmt)
{
	return sqlite3_reset(toref(stmt));
}


JNIEXPORT jint JNICALL Java_org_wwscc_storage_SqliteDatabase_clear_1bindings
  (JNIEnv *env, jobject this, jlong stmt)
{
	return sqlite3_clear_bindings(toref(stmt));
}


JNIEXPORT jint JNICALL Java_org_wwscc_storage_SqliteDatabase_bind_1parameter_1count
	(JNIEnv *env, jobject this, jlong stmt)
{
    return sqlite3_bind_parameter_count(toref(stmt));
}


JNIEXPORT jint JNICALL Java_org_wwscc_storage_SqliteDatabase_column_1count
  (JNIEnv *env, jobject this, jlong stmt)
{
	return sqlite3_column_count(toref(stmt));
}


JNIEXPORT jint JNICALL Java_org_wwscc_storage_SqliteDatabase_column_1type
  (JNIEnv *env, jobject this, jlong stmt, jint col)
{
	return sqlite3_column_type(toref(stmt), col);
}


JNIEXPORT jstring JNICALL Java_org_wwscc_storage_SqliteDatabase_column_1decltype
  (JNIEnv *env, jobject this, jlong stmt, jint col)
{
	return (*env)->NewStringUTF(env, sqlite3_column_decltype(toref(stmt), col));
}


JNIEXPORT jstring JNICALL Java_org_wwscc_storage_SqliteDatabase_column_1name
  (JNIEnv *env, jobject this, jlong stmt, jint col)
{
	return (*env)->NewStringUTF(env, sqlite3_column_name(toref(stmt), col));
}


JNIEXPORT jstring JNICALL Java_org_wwscc_storage_SqliteDatabase_column_1text
  (JNIEnv *env, jobject this, jlong stmt, jint col)
{
	return (*env)->NewStringUTF(env, (const char *)sqlite3_column_text(toref(stmt), col));
}


JNIEXPORT jbyteArray JNICALL Java_org_wwscc_storage_SqliteDatabase_column_1blob
  (JNIEnv *env, jobject this, jlong stmt, jint col)
{
	jsize length;
	jbyteArray jBlob;
	jbyte *a;
	const void *blob = sqlite3_column_blob(toref(stmt), col);
	if (!blob) return NULL;

	length = sqlite3_column_bytes(toref(stmt), col);
	jBlob = (*env)->NewByteArray(env, length);

	a = (*env)->GetPrimitiveArrayCritical(env, jBlob, 0);
	memcpy(a, blob, length);
	(*env)->ReleasePrimitiveArrayCritical(env, jBlob, a, 0);

	return jBlob;
}

JNIEXPORT jdouble JNICALL Java_org_wwscc_storage_SqliteDatabase_column_1double
  (JNIEnv *env, jobject this, jlong stmt, jint col)
{
	return sqlite3_column_double(toref(stmt), col);
}


JNIEXPORT jlong JNICALL Java_org_wwscc_storage_SqliteDatabase_column_1long
  (JNIEnv *env, jobject this, jlong stmt, jint col)
{
	return sqlite3_column_int64(toref(stmt), col);
}


JNIEXPORT jint JNICALL Java_org_wwscc_storage_SqliteDatabase_column_1int
  (JNIEnv *env, jobject this, jlong stmt, jint col)
{
	return sqlite3_column_int(toref(stmt), col);
}


JNIEXPORT jint JNICALL Java_org_wwscc_storage_SqliteDatabase_bind_1null
  (JNIEnv *env, jobject this, jlong stmt, jint col)
{
	return sqlite3_bind_null(toref(stmt), col);
}


JNIEXPORT jint JNICALL Java_org_wwscc_storage_SqliteDatabase_bind_1int
  (JNIEnv *env, jobject this, jlong stmt, jint col, jint val)
{
	return sqlite3_bind_int(toref(stmt), col, val);
}


JNIEXPORT jint JNICALL Java_org_wwscc_storage_SqliteDatabase_bind_1long
  (JNIEnv *env, jobject this, jlong stmt, jint col, jlong val)
{
	return sqlite3_bind_int64(toref(stmt), col, val);
}


JNIEXPORT jint JNICALL Java_org_wwscc_storage_SqliteDatabase_bind_1double
  (JNIEnv *env, jobject this, jlong stmt, jint col, jdouble val)
{
	return sqlite3_bind_double(toref(stmt), col, val);
}


JNIEXPORT jint JNICALL Java_org_wwscc_storage_SqliteDatabase_bind_1text
  (JNIEnv *env, jobject this, jlong stmt, jint col, jstring val)
{
	const char *chars = (*env)->GetStringUTFChars(env, val, 0);
	int rc = sqlite3_bind_text(toref(stmt), col, chars, -1, SQLITE_TRANSIENT);
	(*env)->ReleaseStringUTFChars(env, val, chars);
	return rc;
}


JNIEXPORT jint JNICALL Java_org_wwscc_storage_SqliteDatabase_bind_1blob
  (JNIEnv *env, jobject this, jlong stmt, jint col, jbyteArray val)
{
	int size = (*env)->GetArrayLength(env, val);
	void *ptr = (*env)->GetPrimitiveArrayCritical(env, val, 0);
	int rc = sqlite3_bind_blob(toref(stmt), col, ptr, size, SQLITE_TRANSIENT);
	(*env)->ReleasePrimitiveArrayCritical(env, val, ptr, JNI_ABORT);
	return rc;
}

