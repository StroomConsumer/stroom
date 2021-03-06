package stroom.test;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.util.io.FileUtil;
import stroom.util.shared.Version;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class ContentPackDownloader extends AbstractContentDownloader {

    public static final String URL_PREFIX = "https://github.com/gchq/stroom-content/releases/download/";
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentPackDownloader.class);

    public static Path downloadContentPack(final String contentPackName, final Version version, final Path destDir) {
        return downloadContentPack(contentPackName, version, destDir, ConflictMode.KEEP_EXISTING);
    }

    public static Path downloadContentPack(final String contentPackName, final Version version, final Path destDir, final ConflictMode conflictMode) {
        Preconditions.checkNotNull(contentPackName);
        Preconditions.checkNotNull(version);
        Preconditions.checkNotNull(destDir);
        Preconditions.checkNotNull(conflictMode);
        Preconditions.checkArgument(Files.isDirectory(destDir));

        Path destFilePath = buildDestFilePath(contentPackName, version, destDir);
        boolean destFileExists = Files.isRegularFile(destFilePath);

        if (destFileExists && conflictMode.equals(ConflictMode.KEEP_EXISTING)) {
            LOGGER.debug("Requested contentPack {} already exists in {}, keeping existing",
                    contentPackName,
                    FileUtil.getCanonicalPath(destFilePath));
            return destFilePath;
        }

        if (destFileExists && conflictMode.equals(ConflictMode.OVERWRITE_EXISTING)) {
            LOGGER.debug("Requested contentPack {} already exists in {}, overwriting existing",
                    contentPackName,
                    FileUtil.getCanonicalPath(destFilePath));
            try {
                Files.delete(destFilePath);
            } catch (final IOException e) {
                throw new UncheckedIOException(String.format("Unable to remove existing content pack %s",
                        FileUtil.getCanonicalPath(destFilePath)), e);
            }
        }

        URL fileUrl = buildFileUrl(contentPackName, version);
        LOGGER.info("Downloading contentPack {} from {} to {}",
                contentPackName,
                fileUrl.toString(),
                FileUtil.getCanonicalPath(destFilePath));

        downloadFile(fileUrl, destFilePath);

        return destFilePath;
    }

}
