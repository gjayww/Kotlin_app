object Contract {
    const val DATABASE_NAME = "SuspiciousData.db"
    const val DATABASE_VERSION = 1

    const val TABLE_NAME = "suspicious_data"
    const val COLUMN_PHONE_NUMBER = "phone_number"
    const val COLUMN_REASON = "reason"

    // 建立表格的 SQL 命令
    const val SQL_CREATE_TABLE =
        "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_PHONE_NUMBER TEXT PRIMARY KEY," +
                "$COLUMN_REASON TEXT)"

    // 刪除表格的 SQL 命令
    const val SQL_DELETE_TABLE =
        "DROP TABLE IF EXISTS $TABLE_NAME"
}
