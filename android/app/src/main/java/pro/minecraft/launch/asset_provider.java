package pro.minecraft.launch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.util.Log;

/**
 * Populates the game's asset directory from the APK bundle.
 *
 * <p>Pack the real Minecraft assets into {@code app/src/main/assets/minecraft_assets.zip}
 * with the layout {@code indexes/<id>.json} + {@code objects/<h[:2]>/<h>} + {@code version.json}
 * (the normal client asset folder). When the zip is absent the game still boots; missing
 * assets degrade to placeholders and the README explains how to generate the bundle.</p>
 */
public final class asset_provider {

    private static final String TAG = "asset_provider";
    private static final String BUNDLE = "minecraft_assets.zip";
    private static final String DEFAULT_INDEX = "34";

    private asset_provider() {
    }

    /** @return the asset index id, or null if no bundle was found. */
    public static String extract(Context context, File gameDir) {
        File assetsDir = new File(gameDir, "assets");
        assetsDir.mkdirs();

        InputStream bundle = null;
        try {
            bundle = context.getAssets().open(BUNDLE);
        } catch (Exception e) {
            bundle = null;
        }

        if (bundle == null) {
            Log.w(TAG, "no " + BUNDLE + " in APK assets — booting with placeholder assets");
            new File(assetsDir, "indexes").mkdirs();
            new File(assetsDir, "objects").mkdirs();
            return null;
        }

        try (ZipInputStream zip = new ZipInputStream(bundle)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                File out = new File(assetsDir, entry.getName());
                out.getParentFile().mkdirs();
                try (OutputStream os = new FileOutputStream(out)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = zip.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                }
            }
            Log.i(TAG, "assets extracted to " + assetsDir);
        } catch (Exception e) {
            Log.e(TAG, "failed to extract assets", e);
            return null;
        }

        String index = findIndex(assetsDir);
        return index == null ? DEFAULT_INDEX : index;
    }

    private static String findIndex(File assetsDir) {
        File indexes = new File(assetsDir, "indexes");
        File[] list = indexes.listFiles((dir, name) -> name.endsWith(".json"));
        if (list == null || list.length == 0) {
            return null;
        }
        String name = list[0].getName();
        return name.substring(0, name.length() - ".json".length());
    }
}