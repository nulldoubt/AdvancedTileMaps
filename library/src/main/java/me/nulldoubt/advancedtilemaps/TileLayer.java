package me.nulldoubt.advancedtilemaps;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.UBJsonReader;
import com.badlogic.gdx.utils.UBJsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class TileLayer {

    private static final IntMap<Byte> configuration;
    private static final GridPoint2[] neighbors;

    private static RenderStrategy defaultRenderStrategy;
    private static float insetToleranceX;
    private static float insetToleranceY;
    private static byte zeroIndex;

    static {
        configuration = new IntMap<>(16);
        configuration.put(0b1111, (byte) 6);
        configuration.put(0b0001, (byte) 13);
        configuration.put(0b0010, (byte) 0);
        configuration.put(0b0100, (byte) 8);
        configuration.put(0b1000, (byte) 15);
        configuration.put(0b0101, (byte) 1);
        configuration.put(0b1010, (byte) 11);
        configuration.put(0b0011, (byte) 3);
        configuration.put(0b1100, (byte) 9);
        configuration.put(0b0111, (byte) 5);
        configuration.put(0b1011, (byte) 2);
        configuration.put(0b1101, (byte) 10);
        configuration.put(0b1110, (byte) 7);
        configuration.put(0b0110, (byte) 14);
        configuration.put(0b1001, (byte) 4);
        configuration.put(0b0000, zeroIndex = (byte) 12);

        neighbors = new GridPoint2[]{
            new GridPoint2(0, 0), new GridPoint2(1, 0),
            new GridPoint2(0, 1), new GridPoint2(1, 1)
        };

        defaultRenderStrategy = IntegratedStrategy.VIEW_TILES_VIEW_QUADS;
        insetToleranceX = 0.01f;
        insetToleranceY = 0.01f;
    }

    public static void setAutoTileConfiguration(IntMap<Byte> configuration) {
        TileLayer.configuration.clear(16);
        TileLayer.configuration.putAll(configuration);
        zeroIndex = TileLayer.configuration.get(0b0000);
    }

    /* Re-set your tileSet after using this! */
    public static void setInsetTolerance(float insetToleranceX, float insetToleranceY) {
        TileLayer.insetToleranceX = insetToleranceX;
        TileLayer.insetToleranceY = insetToleranceY;
    }

    public static RenderStrategy getDefaultRenderStrategy() {
        return defaultRenderStrategy;
    }

    public static void setDefaultRenderStrategy(RenderStrategy defaultRenderStrategy) {
        TileLayer.defaultRenderStrategy = defaultRenderStrategy;
    }

    /* Serialization methods */
    public static TileLayer read(FileHandle fileHandle) {
        return read(fileHandle.read());
    }

    public static TileLayer read(InputStream inputStream) {
        final UBJsonReader reader = new UBJsonReader();
        reader.oldFormat = false;

        final JsonValue root = reader.parse(inputStream);

        final TileLayer tileLayer = new TileLayer(
            root.getInt("tilesX"),
            root.getInt("tilesY"),
            root.getInt("tileWidth"),
            root.getInt("tileHeight"),
            root.getFloat("unitScale"),
            false
        );
        tileLayer.setOverlayScale(root.getFloat("overlayScale"));
        tileLayer.setRenderStrategy(IntegratedStrategy.valueOf(root.getString("renderStrategy")));

        boolean[][] tiles = _decompress(root.get("tiles").asByteArray(), tileLayer.tilesX, tileLayer.tilesY);
        for (int x = 0; x < tileLayer.tilesX; x++)
            for (int y = 0; y < tileLayer.tilesY; y++)
                tileLayer.tileAt(x, y, tiles[x][y]);

        return tileLayer;
    }

    public static boolean write(TileLayer tileLayer, FileHandle fileHandle) {
        return write(tileLayer, fileHandle.write(false));
    }

    public static boolean write(TileLayer tileLayer, OutputStream outputStream) {
        try (final UBJsonWriter writer = new UBJsonWriter(outputStream)) {
            writer
                .object()
                .set("tilesX", tileLayer.tilesX)
                .set("tilesY", tileLayer.tilesY)
                .set("tileWidth", tileLayer.tileWidth)
                .set("tileHeight", tileLayer.tileHeight)
                .set("overlayScale", tileLayer.overlayScale)
                .set("unitScale", tileLayer.unitScale)
                .set("renderStrategy", IntegratedStrategy.nameOf(tileLayer.renderStrategy))
                .set("tiles", _compress(tileLayer.tiles, tileLayer.tilesX, tileLayer.tilesY))
                .pop()
                .flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static byte[] _compress(boolean[][] tiles, int tilesX, int tilesY) {
        int totalBits = tilesX * tilesY;
        int totalBytes = (totalBits + 7) / 8;
        byte[] bytes = new byte[totalBytes];

        int bitIndex = 0;
        for (int y = 0; y < tilesY; y++) {
            for (boolean[] tile : tiles) {
                if (tile[y])
                    bytes[bitIndex / 8] |= (byte) (1 << (bitIndex % 8));
                bitIndex++;
            }
        }
        return bytes;
    }

    private static boolean[][] _decompress(byte[] bytes, int tilesX, int tilesY) {
        boolean[][] tiles = new boolean[tilesX][tilesY];

        int bitIndex = 0;
        for (int y = 0; y < tilesY; y++) {
            for (int x = 0; x < tilesX; x++) {
                tiles[x][y] = (bytes[bitIndex / 8] & (1 << (bitIndex % 8))) != 0;
                bitIndex++;
            }
        }
        return tiles;
    }

    private final TextureRegion[] tileSet;
    private final Rectangle viewBounds;
    private Texture texture;

    private Texture overlayTexture;
    private ShaderProgram overlayShaderProgram;
    private boolean overlayed;

    private final int tilesX;
    private final int tilesY;

    private final float tileWidth;
    private final float tileHeight;

    private final float offsetX;
    private final float offsetY;

    private float overlayScale;
    private float unitScale;

    private final boolean[][] tiles;
    private final byte[][] indices;

    private RenderStrategy renderStrategy;
    private int tilesRendered;
    private int quadsRendered;

    public TileLayer(int tilesX, int tilesY, float tileWidth, float tileHeight, float unitScale, boolean fill) {
        this.tilesX = tilesX;
        this.tilesY = tilesY;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.unitScale = unitScale;

        offsetX = tileWidth / 2f;
        offsetY = tileHeight / 2f;

        tiles = new boolean[tilesX][tilesY];
        indices = new byte[tilesX][tilesY];

        tileSet = new TextureRegion[16];
        viewBounds = new Rectangle();

        renderStrategy = defaultRenderStrategy;
        fill(fill);
    }

    public int getTilesX() {
        return tilesX;
    }

    public int getTilesY() {
        return tilesY;
    }

    public float getTileWidth() {
        return tileWidth;
    }

    public float getTileHeight() {
        return tileHeight;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public float getUnitScale() {
        return unitScale;
    }

    public void setUnitScale(float unitScale) {
        this.unitScale = unitScale;
    }

    public float getOverlayScale() {
        return overlayScale;
    }

    public void setOverlayScale(float overlayScale) {
        this.overlayScale = overlayScale;
    }

    public boolean hasOverlay() {
        return overlayed;
    }

    public Texture getOverlayTexture() {
        return overlayTexture;
    }

    public ShaderProgram getOverlayShaderProgram() {
        return overlayShaderProgram;
    }

    public void setOverlay(Texture overlayTexture, ShaderProgram overlayShaderProgram) {
        this.overlayTexture = overlayTexture;
        this.overlayShaderProgram = overlayShaderProgram;
        overlayed = (overlayTexture != null && overlayShaderProgram != null);
        if (overlayed)
            overlayScale = 1f / overlayTexture.getWidth();
    }

    public boolean hasTileSet() {
        return texture != null;
    }

    public Texture getTileSetTexture() {
        return texture;
    }

    public void setTileSet(final TextureRegion textureRegion) {
        texture = textureRegion.getTexture();
        final float tileSetU = textureRegion.getU();
        final float tileSetV = textureRegion.getV();
        final float width = tileWidth / texture.getWidth();
        final float height = tileHeight / texture.getHeight();
        final float insetX = insetToleranceX / texture.getWidth();
        final float insetY = insetToleranceY / texture.getHeight();
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) {
                final float u = tileSetU + i * width + insetX;
                final float v = tileSetV + j * height + insetY;
                tileSet[i + j * 4] = new TextureRegion(texture, u, v, u + width - 2 * insetX, v + height - 2 * insetY);
            }
    }

    public Rectangle getViewBounds() {
        return viewBounds;
    }

    public int getTilesRendered() {
        return tilesRendered;
    }

    public int getQuadsRendered() {
        return quadsRendered;
    }

    public RenderStrategy getRenderStrategy() {
        return renderStrategy;
    }

    public void setRenderStrategy(RenderStrategy renderStrategy) {
        this.renderStrategy = renderStrategy;
    }

    public void fill(boolean state) {
        for (final boolean[] row : tiles)
            Arrays.fill(row, state);
        final byte tile = configuration.get(state ? 0b1111 : 0b0000);
        for (final byte[] row : indices)
            Arrays.fill(row, tile);
    }

    public boolean isOutOfBounds(final int x, final int y) {
        return (x < 0 || y < 0 || x >= tilesX || y >= tilesY);
    }

    public boolean tileAt(final int x, final int y) {
        if (isOutOfBounds(x, y))
            return false;
        return tiles[x][y];
    }

    public void tileAt(final int x, final int y, final boolean state) {
        if (isOutOfBounds(x, y))
            return;
        tiles[x][y] = state;
        for (final GridPoint2 neighbor : neighbors) {
            final int nX = x + neighbor.x;
            final int nY = y + neighbor.y;
            if (isOutOfBounds(nX, nY))
                continue;

            int bitmask = 0;
            bitmask |= tileAt(nX - neighbors[1].x, nY - neighbors[1].y) ? (1 << 3) : 0;
            bitmask |= tileAt(nX - neighbors[0].x, nY - neighbors[0].y) ? (1 << 2) : 0;
            bitmask |= tileAt(nX - neighbors[3].x, nY - neighbors[3].y) ? (1 << 1) : 0;
            bitmask |= tileAt(nX - neighbors[2].x, nY - neighbors[2].y) ? (1) : 0;
            indices[nX][nY] = configuration.get(bitmask);
        }
    }

    /* May be called before rendering! */
    public void setView(OrthographicCamera camera) {
        float width = camera.viewportWidth * camera.zoom;
        float height = camera.viewportHeight * camera.zoom;
        float w = width * Math.abs(camera.up.y) + height * Math.abs(camera.up.x);
        float h = height * Math.abs(camera.up.y) + width * Math.abs(camera.up.x);
        viewBounds.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h);
    }

    /* May be called before rendering! */
    public void setView(float x, float y, float width, float height) {
        viewBounds.set(x, y, width, height);
    }

    public void render(final Batch batch) {
        if (texture == null)
            return;

        if (overlayed) {
            overlayTexture.bind(1);
            texture.bind(0);
            overlayShaderProgram.bind();
            overlayShaderProgram.setUniformi("u_overlay", 1);
            overlayShaderProgram.setUniformi("u_texture", 0);
            overlayShaderProgram.setUniformf("u_scale", overlayScale / unitScale);
            batch.setShader(overlayShaderProgram);
        }

        renderStrategy.render(this, batch);

        if (overlayed)
            batch.setShader(null);
    }

    public enum IntegratedStrategy implements RenderStrategy {

        ALL_TILES_ALL_QUADS() {
            @Override
            public void render(TileLayer tileLayer, Batch batch) {
                final float tileWidth = tileLayer.tileWidth * tileLayer.unitScale;
                final float tileHeight = tileLayer.tileHeight * tileLayer.unitScale;
                tileLayer.tilesRendered = 0;
                tileLayer.quadsRendered = 0;
                for (int x = 0; x < tileLayer.tilesX; x++) {
                    for (int y = 0; y < tileLayer.tilesY; y++) {
                        if (tileLayer.tiles[x][y])
                            tileLayer.tilesRendered++;
                        tileLayer.quadsRendered++;
                        batch.draw(tileLayer.tileSet[tileLayer.indices[x][y]],
                            (tileLayer.offsetX + x * tileLayer.tileWidth) * tileLayer.unitScale,
                            (tileLayer.offsetY + y * tileLayer.tileHeight) * tileLayer.unitScale,
                            tileWidth, tileHeight
                        );
                    }
                }
            }
        },

        ALL_TILES_VIEW_QUADS() {
            @Override
            public void render(TileLayer tileLayer, Batch batch) {
                final float tileWidth = tileLayer.tileWidth * tileLayer.unitScale;
                final float tileHeight = tileLayer.tileHeight * tileLayer.unitScale;
                tileLayer.tilesRendered = 0;
                tileLayer.quadsRendered = 0;
                byte index;
                for (int x = 0; x < tileLayer.tilesX; x++) {
                    for (int y = 0; y < tileLayer.tilesY; y++) {
                        if (tileLayer.tiles[x][y])
                            tileLayer.tilesRendered++;
                        index = tileLayer.indices[x][y];
                        if (index == zeroIndex)
                            continue;
                        tileLayer.quadsRendered++;
                        batch.draw(tileLayer.tileSet[index],
                            (tileLayer.offsetX + x * tileLayer.tileWidth) * tileLayer.unitScale,
                            (tileLayer.offsetY + y * tileLayer.tileHeight) * tileLayer.unitScale,
                            tileWidth, tileHeight
                        );
                    }
                }
            }
        },

        VIEW_TILES_ALL_QUADS() {
            @Override
            public void render(TileLayer tileLayer, Batch batch) {
                final float tileWidth = tileLayer.tileWidth * tileLayer.unitScale;
                final float tileHeight = tileLayer.tileHeight * tileLayer.unitScale;
                int col1 = Math.max(0, (int) ((tileLayer.viewBounds.x - tileLayer.offsetX) / (tileWidth)));
                int col2 = Math.min(tileLayer.tilesX, (int) ((tileLayer.viewBounds.x + tileLayer.viewBounds.width) / (tileWidth)) + 1);
                int row1 = Math.max(0, (int) ((tileLayer.viewBounds.y - tileLayer.offsetY) / (tileHeight)));
                int row2 = Math.min(tileLayer.tilesY, (int) ((tileLayer.viewBounds.y + tileLayer.viewBounds.height) / (tileHeight)) + 1);
                tileLayer.tilesRendered = 0;
                tileLayer.quadsRendered = 0;
                for (int x = col1; x < col2; x++) {
                    for (int y = row1; y < row2; y++) {
                        if (tileLayer.tiles[x][y])
                            tileLayer.tilesRendered++;
                        tileLayer.quadsRendered++;
                        batch.draw(tileLayer.tileSet[tileLayer.indices[x][y]],
                            (tileLayer.offsetX + x * tileLayer.tileWidth) * tileLayer.unitScale,
                            (tileLayer.offsetY + y * tileLayer.tileHeight) * tileLayer.unitScale,
                            tileWidth, tileHeight
                        );
                    }
                }
            }
        },

        VIEW_TILES_VIEW_QUADS() {
            @Override
            public void render(TileLayer tileLayer, Batch batch) {
                final float tileWidth = tileLayer.tileWidth * tileLayer.unitScale;
                final float tileHeight = tileLayer.tileHeight * tileLayer.unitScale;
                int col1 = Math.max(0, (int) ((tileLayer.viewBounds.x - tileLayer.offsetX) / (tileWidth)));
                int col2 = Math.min(tileLayer.tilesX, (int) ((tileLayer.viewBounds.x + tileLayer.viewBounds.width) / (tileWidth)) + 1);
                int row1 = Math.max(0, (int) ((tileLayer.viewBounds.y - tileLayer.offsetY) / (tileHeight)));
                int row2 = Math.min(tileLayer.tilesY, (int) ((tileLayer.viewBounds.y + tileLayer.viewBounds.height) / (tileHeight)) + 1);
                tileLayer.tilesRendered = 0;
                tileLayer.quadsRendered = 0;
                byte index;
                for (int x = col1; x < col2; x++) {
                    for (int y = row1; y < row2; y++) {
                        if (tileLayer.tiles[x][y])
                            tileLayer.tilesRendered++;
                        index = tileLayer.indices[x][y];
                        if (index == zeroIndex)
                            continue;
                        tileLayer.quadsRendered++;
                        batch.draw(tileLayer.tileSet[index],
                            (tileLayer.offsetX + x * tileLayer.tileWidth) * tileLayer.unitScale,
                            (tileLayer.offsetY + y * tileLayer.tileHeight) * tileLayer.unitScale,
                            tileWidth, tileHeight
                        );
                    }
                }
            }
        };

        public static String nameOf(RenderStrategy renderStrategy) {
            if (renderStrategy instanceof TileLayer.IntegratedStrategy)
                return ((TileLayer.IntegratedStrategy) renderStrategy).name();
            return renderStrategy.getClass().getSimpleName();
        }

    }

    public interface RenderStrategy {

        void render(TileLayer tileLayer, Batch batch);

    }

}
