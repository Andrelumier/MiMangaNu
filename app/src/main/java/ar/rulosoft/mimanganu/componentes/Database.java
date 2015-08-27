package ar.rulosoft.mimanganu.componentes;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.fedorvlasov.lazylist.FileCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class Database extends SQLiteOpenHelper {

    // Table for entire manga
    public static final String TABLE_MANGA = "manga";
    public static final String COL_SERVER_ID = "server_id";
    public static final String COL_NAME = "nombre";
    public static final String COL_PATH = "path";
    public static final String COL_IMAGE = "imagen";
    public static final String COL_SYNOPSIS = "sinopsis";
    public static final String COL_LAST_READ = "ultima";
    public static final String COL_ID = "id";
    public static final String COL_LAST_INDEX = "last_index";// indice listview
    public static final String COL_NEW = "nuevos";// hay nuevos?
    public static final String COL_SEARCH = "burcar";// buscar updates
    public static final String COL_AUTHOR = "autor";
    // Table for each chapter
    public static final String TABLE_CHAPTERS = "capitulos";
    public static final String COL_CAP_ID_MANGA = "manga_id";
    public static final String COL_CAP_NAME = "nombre";
    public static final String COL_CAP_PATH = "path";
    public static final String COL_CAP_PAGES = "paginas";
    public static final String COL_CAP_PAG_READ = "leidas";
    public static final String COL_CAP_STATE = "estado";
    public static final String COL_CAP_DOWNLOADED = "descargado";
    public static final String COL_CAP_ID = "id";
    private static final String COL_READ_ORDER = "orden_lectura";// sentido de
    // Database creation sql statement
    private static final String DATABASE_MANGA_CREATE = "create table " +
            TABLE_MANGA + "(" +
            COL_ID + " integer primary key autoincrement, " +
            COL_NAME + " text not null," +
            COL_PATH + " text not null UNIQUE, " +
            COL_IMAGE + " text," +
            COL_SYNOPSIS + " text," +
            COL_SERVER_ID + "," +
            COL_LAST_READ + " int," +
            COL_NEW + " int DEFAULT 0," +
            COL_LAST_INDEX + " int DEFAULT 0, " +
            COL_SEARCH + " int DEFAULT 0, " +
            COL_READ_ORDER + " int not null DEFAULT -1, " +
            COL_AUTHOR + " TEXT NOT NULL DEFAULT 'N/A');";
    private static final String DATABASE_CAPITULOS_CREATE = "create table " +
            TABLE_CHAPTERS + "(" +
            COL_CAP_ID + " integer primary key autoincrement, " +
            COL_CAP_NAME + " text not null," +
            COL_CAP_PATH + " text not null UNIQUE, " +
            COL_CAP_PAGES + " int," +
            COL_CAP_ID_MANGA + " int," +
            COL_CAP_STATE + " int DEFAULT 0," +
            COL_CAP_PAG_READ + " int DEFAULT 1, " +
            COL_CAP_DOWNLOADED + " int DEFAULT 0);";
    // name and path of database
    private static String database_name;
    private static String database_path;
    private static int database_version = 9;
    private static SQLiteDatabase localDB;
    Context context;

    // make private, should be single instance
    private Database(Context context) {

        super(context, (PreferenceManager.getDefaultSharedPreferences(context).getString("directorio", Environment.getExternalStorageDirectory().getAbsolutePath()) + "/MiMangaNu/dbs/") + database_name, null, database_version);
        this.context = context;
    }

    public static SQLiteDatabase getDatabase(Context c) {
        // Setup path and database name
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        database_path = (prefs.getString("directorio",
                Environment.getExternalStorageDirectory().getAbsolutePath()) + "/MiMangaNu/") + "dbs/";
        database_name = "mangas.db";
        if ((localDB == null) || !localDB.isOpen()) {
            localDB = new Database(c).getReadableDatabase();
        }
        return localDB;
    }

    public static int addManga(Context c, Manga m) {
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME, m.title);
        cv.put(COL_PATH, m.path);
        cv.put(COL_IMAGE, m.images);
        cv.put(COL_SYNOPSIS, m.synopsis);
        cv.put(COL_SERVER_ID, m.serverId);
        cv.put(COL_AUTHOR, m.getAuthor());
        cv.put(COL_LAST_READ, System.currentTimeMillis());

        if (m.finished)
            cv.put(COL_SEARCH, 1);
        else
            cv.put(COL_SEARCH, 0);

        return (int) getDatabase(c).insert(TABLE_MANGA, null, cv);
    }

    public static void updateManga(Context context, Manga manga) {
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME, manga.title);
        cv.put(COL_PATH, manga.path);
        cv.put(COL_IMAGE, manga.images);
        cv.put(COL_SYNOPSIS, manga.synopsis);
        cv.put(COL_SERVER_ID, manga.serverId);
        cv.put(COL_AUTHOR, manga.getAuthor());
        cv.put(COL_LAST_READ, System.currentTimeMillis());

        if (manga.finished)
            cv.put(COL_SEARCH, 1);
        else
            cv.put(COL_SEARCH, 0);

        getDatabase(context).update(TABLE_MANGA, cv, COL_ID + "=" + manga.getId(), null);
    }

    public static void updateMangaNotime(Context context, Manga manga) {
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME, manga.title);
        cv.put(COL_PATH, manga.path);
        cv.put(COL_IMAGE, manga.images);
        cv.put(COL_SYNOPSIS, manga.synopsis);
        cv.put(COL_SERVER_ID, manga.serverId);
        cv.put(COL_AUTHOR, manga.getAuthor());

        if (manga.finished)
            cv.put(COL_SEARCH, 1);
        else
            cv.put(COL_SEARCH, 0);

        getDatabase(context).update(TABLE_MANGA, cv, COL_ID + "=" + manga.getId(), null);
    }

    public static void updateMangaRead(Context c, int mid) {
        ContentValues cv = new ContentValues();
        cv.put(COL_LAST_READ, System.currentTimeMillis());
        getDatabase(c).update(TABLE_MANGA, cv, COL_ID + "=" + mid, null);
    }

    public static void setUpgradable(Context c, int mangaid, boolean buscar) {
        ContentValues cv = new ContentValues();
        if (buscar)
            cv.put(COL_SEARCH, 1);
        else
            cv.put(COL_SEARCH, 0);
        getDatabase(c).update(TABLE_MANGA, cv, COL_ID + "=" + mangaid, null);
    }

    public static void updateMangaLastIndex(Context c, int mid, int idx) {
        ContentValues cv = new ContentValues();
        cv.put(COL_LAST_INDEX, idx);
        getDatabase(c).update(TABLE_MANGA, cv, COL_ID + "=" + mid, null);
    }

    public static void updateNewMangas(Context c, Manga m, int nuevos) {
        int actual = 0;
        if (nuevos > -99) {
            Cursor cursor = getDatabase(c).query(TABLE_MANGA, new String[]{COL_NEW}, COL_ID + " = " + m.id, null, null, null, null);
            if (cursor.moveToFirst()) {
                actual = cursor.getInt(cursor.getColumnIndex(COL_NEW));
                actual += nuevos;
            }
            cursor.close();
        }
        ContentValues cv = new ContentValues();
        if (!(actual > 0))
            cv.put(COL_NEW, 0);
        else
            cv.put(COL_NEW, actual);
        getDatabase(c).update(TABLE_MANGA, cv, COL_ID + "=" + m.getId(), null);
    }

    public static void addChapter(Context c, Chapter cap, int mangaId) {
        ContentValues cv = new ContentValues();
        cv.put(COL_CAP_ID_MANGA, mangaId);
        cv.put(COL_CAP_NAME, cap.getTitle());
        cv.put(COL_CAP_PATH, cap.path);
        cv.put(COL_CAP_PAGES, cap.getPages());
        cv.put(COL_CAP_STATE, cap.getReadStatus());
        cv.put(COL_CAP_PAG_READ, cap.getPagesRead());
        getDatabase(c).insert(TABLE_CHAPTERS, null, cv);
    }

    public static void updateChapter(Context context, Chapter cap) {
        ContentValues cv = new ContentValues();
        cv.put(COL_CAP_NAME, cap.getTitle());
        cv.put(COL_CAP_PATH, cap.path);
        cv.put(COL_CAP_PAGES, cap.getPages());
        cv.put(COL_CAP_STATE, cap.getReadStatus());
        cv.put(COL_CAP_PAG_READ, cap.getPagesRead());
        getDatabase(context).update(TABLE_CHAPTERS, cv, COL_CAP_ID + " = " + cap.id, null);
    }

    public static ArrayList<Manga> getMangasForUpdates(Context c) {
        return getMangasCondition(c, COL_SEARCH + "= 0");
    }

    public static ArrayList<Manga> getMangas(Context c) {
        return getMangasCondition(c, null);
    }

    public static ArrayList<Manga> getMangasCondition(Context c, String condition) {
        Cursor cursor = getDatabase(c).query(
                TABLE_MANGA,
                new String[]{COL_ID, COL_NAME, COL_PATH, COL_IMAGE, COL_SYNOPSIS, COL_LAST_READ, COL_SERVER_ID, COL_NEW, COL_SEARCH, COL_LAST_INDEX,
                        COL_READ_ORDER, COL_AUTHOR}, condition, null, null, null, COL_LAST_READ + " DESC");
        return getMangasFromCursor(cursor);
    }

    public static ArrayList<Manga> getMangasFromCursor(Cursor cursor) {
        ArrayList<Manga> mangas = new ArrayList<>();
        if (cursor.moveToFirst()) {
            int colId = cursor.getColumnIndex(COL_ID);
            int colServerId = cursor.getColumnIndex(COL_SERVER_ID);
            int colTitulo = cursor.getColumnIndex(COL_NAME);
            int colSinopsis = cursor.getColumnIndex(COL_SYNOPSIS);
            int colImagen = cursor.getColumnIndex(COL_IMAGE);
            int colWeb = cursor.getColumnIndex(COL_PATH);
            int conNuevos = cursor.getColumnIndex(COL_NEW);
            int colBuscar = cursor.getColumnIndex(COL_SEARCH);
            int colLastIdx = cursor.getColumnIndex(COL_LAST_INDEX);
            int colSentido = cursor.getColumnIndex(COL_READ_ORDER);
            int colAutor = cursor.getColumnIndex(COL_AUTHOR);

            do {
                Manga m = new Manga(cursor.getInt(colServerId), cursor.getString(colTitulo), cursor.getString(colWeb), false);
                m.setSinopsis(cursor.getString(colSinopsis));
                m.setImages(cursor.getString(colImagen));
                m.setId(cursor.getInt(colId));
                m.setNews(cursor.getInt(conNuevos));
                m.setFinished(cursor.getInt(colBuscar) > 0);
                m.setLastIndex(cursor.getInt(colLastIdx));
                m.setReadingDirection(cursor.getInt(colSentido));
                m.setAuthor(cursor.getString(colAutor));
                mangas.add(m);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return mangas;
    }

    public static Manga getFullManga(Context c, int mangaID) {
        Manga manga = null;
        try {
            Manga m = getMangasCondition(c, COL_ID + "=" + mangaID).get(0);
            m.setChapters(getChapters(c, mangaID));
            manga = m;
        } catch (Exception e) {
        }
        return manga;
    }

    public static Manga getFullManga(Context c, int mangaID, boolean asc) {
        Manga manga = null;
        try {
            Manga m = getMangasCondition(c, COL_ID + "=" + mangaID).get(0);
            m.setChapters(getChapters(c, mangaID, "1", asc));
            manga = m;
        } catch (Exception e) {
        }
        return manga;
    }

    public static ArrayList<Chapter> getChapters(Context c, int MangaId) {
        return getChapters(c, MangaId, "1");
    }

    public static ArrayList<Chapter> getChapters(Context c, int MangaId, String condicion) {
        return getChapters(c, MangaId, condicion, false);
    }

    public static ArrayList<Chapter> getChapters(Context c, int MangaId, String condicion, boolean asc) {
        ArrayList<Chapter> chapters = new ArrayList<>();
        String order = " DESC";
        if (asc)
            order = " ASC";
        Cursor cursor = getDatabase(c).query(
                TABLE_CHAPTERS,
                new String[]{COL_CAP_ID, COL_CAP_ID_MANGA, COL_CAP_NAME, COL_CAP_PATH, COL_CAP_PAGES, COL_CAP_PAG_READ, COL_CAP_STATE,
                        COL_CAP_DOWNLOADED}, COL_CAP_ID_MANGA + "=" + MangaId + " AND " + condicion, null, null, null, COL_CAP_ID + order);
        if (cursor.moveToFirst()) {
            int colId = cursor.getColumnIndex(COL_CAP_ID);
            int colTitle = cursor.getColumnIndex(COL_CAP_NAME);
            int colPages = cursor.getColumnIndex(COL_CAP_PAGES);
            int colWeb = cursor.getColumnIndex(COL_CAP_PATH);
            int colPageRead = cursor.getColumnIndex(COL_CAP_PAG_READ);
            int colState = cursor.getColumnIndex(COL_CAP_STATE);
            int colDownloaded = cursor.getColumnIndex(COL_CAP_DOWNLOADED);
            do {
                Chapter cap = new Chapter(cursor.getString(colTitle), cursor.getString(colWeb));
                cap.setPages(cursor.getInt(colPages));
                cap.setId(cursor.getInt(colId));
                cap.setMangaID(MangaId);
                cap.setReadStatus(cursor.getInt(colState));
                cap.setPagesRead(cursor.getInt(colPageRead));
                cap.setDownloaded((cursor.getInt(colDownloaded) == 1));
                chapters.add(cap);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return chapters;
    }

    public static Chapter getChapter(Context c, int capId) {
        Chapter cap = null;
        Cursor cursor = getDatabase(c).query(
                TABLE_CHAPTERS,
                new String[]{COL_CAP_ID, COL_CAP_ID_MANGA, COL_CAP_NAME, COL_CAP_PATH, COL_CAP_PAGES, COL_CAP_PAG_READ, COL_CAP_STATE,
                        COL_CAP_DOWNLOADED}, COL_CAP_ID + "=" + capId, null, null, null, null);
        if (cursor.moveToFirst()) {
            int colId = cursor.getColumnIndex(COL_CAP_ID);
            int colMID = cursor.getColumnIndex(COL_CAP_ID_MANGA);
            int colTitle = cursor.getColumnIndex(COL_CAP_NAME);
            int colPages = cursor.getColumnIndex(COL_CAP_PAGES);
            int colWeb = cursor.getColumnIndex(COL_CAP_PATH);
            int colPageRead = cursor.getColumnIndex(COL_CAP_PAG_READ);
            int colState = cursor.getColumnIndex(COL_CAP_STATE);
            int colDownloaded = cursor.getColumnIndex(COL_CAP_DOWNLOADED);

            cap = new Chapter(cursor.getString(colTitle), cursor.getString(colWeb));
            cap.setPages(cursor.getInt(colPages));
            cap.setId(cursor.getInt(colId));
            cap.setMangaID(cursor.getInt(colMID));
            cap.setReadStatus(cursor.getInt(colState));
            cap.setPagesRead(cursor.getInt(colPageRead));
            cap.setDownloaded((cursor.getInt(colDownloaded) == 1));
        }
        cursor.close();
        return cap;
    }

    public static void updateChapterDownloaded(Context c, int cid, int state) {
        ContentValues cv = new ContentValues();
        cv.put(COL_CAP_DOWNLOADED, state);
        getDatabase(c).update(TABLE_CHAPTERS, cv, COL_CAP_ID + "=" + Integer.toString(cid), null);
    }

    public static void updateChapterPlusDownload(Context context, Chapter cap) {
        ContentValues cv = new ContentValues();
        cv.put(COL_CAP_NAME, cap.getTitle());
        cv.put(COL_CAP_PATH, cap.path);
        cv.put(COL_CAP_PAGES, cap.getPages());
        cv.put(COL_CAP_STATE, cap.getReadStatus());
        cv.put(COL_CAP_PAG_READ, cap.getPagesRead());
        cv.put(COL_CAP_DOWNLOADED, cap.isDownloaded() ? 1 : 0);
        getDatabase(context).update(TABLE_CHAPTERS, cv, COL_CAP_ID + " = " + cap.id, null);

    }

    public static void updateChapterPage(Context c, int cid, int pages) {
        ContentValues cv = new ContentValues();
        cv.put(COL_CAP_PAG_READ, pages);
        getDatabase(c).update(TABLE_CHAPTERS, cv, COL_CAP_ID + "=" + Integer.toString(cid), null);
    }

    public static void deleteManga(Context c, int mid) {
        getDatabase(c).delete(TABLE_MANGA, COL_ID + " = " + mid, null);
        getDatabase(c).delete(TABLE_CHAPTERS, COL_CAP_ID_MANGA + "=" + mid, null);
    }

    public static void deleteChapter(Context context, Chapter chapter) {
        getDatabase(context).delete(TABLE_CHAPTERS, COL_CAP_ID + "=" + chapter.id, null);
    }

    public static Manga getManga(Context context, int mangaID) {
        Manga manga = null;
        Cursor cursor = getDatabase(context).query(TABLE_MANGA,
                new String[]{COL_ID, COL_NAME, COL_PATH, COL_IMAGE, COL_SYNOPSIS, COL_LAST_READ, COL_SERVER_ID, COL_NEW, COL_LAST_INDEX, COL_READ_ORDER, COL_AUTHOR},
                COL_ID + "=" + mangaID, null, null, null, COL_LAST_READ + " DESC");
        if (cursor.moveToFirst()) {
            int colId = cursor.getColumnIndex(COL_ID);
            int colServerId = cursor.getColumnIndex(COL_SERVER_ID);
            int colTitle = cursor.getColumnIndex(COL_NAME);
            int colSummary = cursor.getColumnIndex(COL_SYNOPSIS);
            int colImages = cursor.getColumnIndex(COL_IMAGE);
            int colWeb = cursor.getColumnIndex(COL_PATH);
            int colNew = cursor.getColumnIndex(COL_NEW);
            int colLastIndex = cursor.getColumnIndex(COL_LAST_INDEX);
            int colReadSense = cursor.getColumnIndex(COL_READ_ORDER);
            int colAuthor = cursor.getColumnIndex(COL_AUTHOR);

            Manga m = new Manga(cursor.getInt(colServerId), cursor.getString(colTitle), cursor.getString(colWeb), false);
            m.setSinopsis(cursor.getString(colSummary));
            m.setImages(cursor.getString(colImages));
            m.setId(cursor.getInt(colId));
            m.setNews(cursor.getInt(colNew));
            m.setLastIndex(cursor.getInt(colLastIndex));
            m.setReadingDirection(cursor.getInt(colReadSense));
            m.setAuthor(cursor.getString(colAuthor));

            manga = m;
        }
        cursor.close();
        return manga;
    }

    public static void markChapter(Context c, int capId, boolean read) {
        ContentValues cv = new ContentValues();
        cv.put(COL_CAP_STATE, read ? Chapter.READ : Chapter.UNREAD);
        getDatabase(c).update(TABLE_CHAPTERS, cv, COL_CAP_ID + " = " + capId, null);
    }

    public static void markAllChapters(Context c, int mangaId, boolean read) {
        ContentValues cv = new ContentValues();
        cv.put(COL_CAP_STATE, read ? Chapter.READ : Chapter.UNREAD);
        getDatabase(c).update(TABLE_CHAPTERS, cv, COL_CAP_ID_MANGA + " = " + mangaId, null);
    }

    public static void removeOrphanedChapters(Context c) {
        getDatabase(c).delete(TABLE_CHAPTERS, COL_CAP_ID_MANGA + "= -1", null);
    }

    public static void updadeReadOrder(Context c, int ordinal, int mid) {
        ContentValues cv = new ContentValues();
        cv.put(COL_READ_ORDER, ordinal);
        getDatabase(c).update(TABLE_MANGA, cv, COL_ID + "=" + mid, null);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (context.getDatabasePath("mangas.db").exists() && !(doesTableExist(db, TABLE_MANGA))) {
            //move to new path
            copyDbToSd(context);
            db.close();
            //restart app
            Intent i = context.getPackageManager()
                    .getLaunchIntentForPackage(context.getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(i);
            System.exit(0);
        } else {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            File destination = new File(sp.getString("directorio", Environment.getExternalStorageDirectory().getAbsolutePath()) + "/MiMangaNu/dbs");
            destination.mkdirs();
            db.execSQL(DATABASE_MANGA_CREATE);
            db.execSQL(DATABASE_CAPITULOS_CREATE);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void copyDbToSd(Context c) {
        File dbFile = c.getDatabasePath("mangas.db");
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        String ruta = sp.getString("directorio", Environment.getExternalStorageDirectory().getAbsolutePath()) + "/MiMangaNu/";
        ruta += "dbs/";
        File exportDir = new File(ruta, "");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        File file = new File(exportDir, dbFile.getName());
        try {
            file.createNewFile();
            InputStream is = new FileInputStream(dbFile);
            FileCache.writeFile(is, file);
            is.close();
        } catch (IOException e) {
            Toast.makeText(c, "Error: ", Toast.LENGTH_LONG).show();
        }
    }

    public boolean doesTableExist(SQLiteDatabase db, String tableName) {
        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + tableName + "'", null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }
}
