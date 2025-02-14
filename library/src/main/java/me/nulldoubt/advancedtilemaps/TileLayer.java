package me.nulldoubt.advancedtilemaps;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.IntMap;

import java.util.Arrays;

public class TileLayer {

    private static final IntMap<Byte> configuration;
    private static final GridPoint2[] neighbors;
    private static float insetToleranceX;
    private static float insetToleranceY;

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
        configuration.put(0b0000, (byte) 12);

        neighbors = new GridPoint2[]{
            new GridPoint2(0, 0), new GridPoint2(1, 0),
            new GridPoint2(0, 1), new GridPoint2(1, 1)
        };

        insetToleranceX = 0.01f;
        insetToleranceY = 0.01f;
    }

    public static void setAutoTileConfiguration(IntMap<Byte> configuration) {
        TileLayer.configuration.clear(16);
        TileLayer.configuration.putAll(configuration);
    }

    /* Re-set your tileSet after using this! */
    public static void setInsetTolerance(float insetToleranceX, float insetToleranceY) {
        TileLayer.insetToleranceX = insetToleranceX;
        TileLayer.insetToleranceY = insetToleranceY;
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
    private int tilesRendered;

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

        renderTiles(batch);

        if (overlayed)
            batch.setShader(null);
    }

    private void renderTiles(Batch batch) {

        int col1 = Math.max(0, (int) ((viewBounds.x - offsetX) / (tileWidth * unitScale)));
        int col2 = Math.min(tilesX, (int) ((viewBounds.x + viewBounds.width) / (tileWidth * unitScale)) + 1);

        int row1 = Math.max(0, (int) ((viewBounds.y - offsetY) / (tileHeight * unitScale)));
        int row2 = Math.min(tilesY, (int) ((viewBounds.y + viewBounds.height) / (tileHeight * unitScale)) + 1);

        tilesRendered = 0;
        for (int x = col1; x < col2; x++) {
            for (int y = row1; y < row2; y++) {
                tilesRendered++;
                batch.draw(tileSet[indices[x][y]],
                    (offsetX + x * tileWidth) * unitScale,
                    (offsetY + y * tileHeight) * unitScale,
                    tileWidth * unitScale, tileHeight * unitScale);
            }
        }
    }

}
