package io.github.appunnim.businessgate.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class GateDbHelper extends SQLiteOpenHelper {
    private final Context context;
    public GateDbHelper(Context context) { super(context, "gate.db", null, 1); this.context = context; setWriteAheadLoggingEnabled(true); }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        try (var stream = context.getAssets().open("schema.sql")) {
            for (String statement : new String(io.github.appunnim.businessgate.support.Bytes.read(stream), StandardCharsets.UTF_8).split(";"))
                if (!statement.trim().isEmpty()) db.execSQL(statement);
        } catch (IOException error) { throw new IllegalStateException("SCHEMA_UNAVAILABLE"); }
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 is the first shipped schema. Never guess or destructively reset a predecessor.
        throw new IllegalStateException("MIGRATION_UNSUPPORTED");
    }
    @Override public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) { throw new IllegalStateException("DOWNGRADE_UNSUPPORTED"); }
}
