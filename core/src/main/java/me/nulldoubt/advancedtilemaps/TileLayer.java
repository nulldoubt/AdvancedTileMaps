package me.nulldoubt.advancedtilemaps;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.IntMap;

import java.util.Arrays;

public class TileLayer {

    private static final IntMap<Byte> conversions;
    private static final GridPoint2[] neighbors;

    static {
        conversions = new IntMap<>(16);
        conversions.put(0b1111, (byte) 6);
        conversions.put(0b0001, (byte) 13);
        conversions.put(0b0010, (byte) 0);
        conversions.put(0b0100, (byte) 8);
        conversions.put(0b1000, (byte) 15);
        conversions.put(0b0101, (byte) 1);
        conversions.put(0b1010, (byte) 11);
        conversions.put(0b0011, (byte) 3);
        conversions.put(0b1100, (byte) 9);
        conversions.put(0b0111, (byte) 5);
        conversions.put(0b1011, (byte) 2);
        conversions.put(0b1101, (byte) 10);
        conversions.put(0b1110, (byte) 7);
        conversions.put(0b0110, (byte) 14);
        conversions.put(0b1001, (byte) 4);
        conversions.put(0b0000, (byte) 12);

        neighbors = new GridPoint2[]{
            new GridPoint2(0, 0), new GridPoint2(1, 0),
            new GridPoint2(0, 1), new GridPoint2(1, 1)
        };
    }

    private final TextureRegion[] tileSet;

    private final Texture texture;
    private final Texture overlay;
    private final ShaderProgram shader;

    public final int tilesX;
    public final int tilesY;

    public final float tileWidth;
    public final float tileHeight;

    public final float offsetX;
    public final float offsetY;

    public float overlayScale;
    public float unitScale;

    private final boolean[][] tiles;
    private final byte[][] indices;

    public TileLayer(final TextureRegion tileSet, final Texture overlay, final ShaderProgram shader, final int tilesX, final int tilesY, final float tileWidth, final float tileHeight, final float unitScale) {
        this.texture = tileSet.getTexture();
        this.overlay = overlay;
        this.shader = shader;

        this.tilesX = tilesX;
        this.tilesY = tilesY;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;

        offsetX = tileWidth / 2f;
        offsetY = tileHeight / 2f;

        overlayScale = 1f / overlay.getWidth();
        this.unitScale = unitScale;

        tiles = new boolean[tilesX][tilesY];
        indices = new byte[tilesX][tilesY];

        this.tileSet = new TextureRegion[16];
        final Texture texture = tileSet.getTexture();
        final float tileSetU = tileSet.getU();
        final float tileSetV = tileSet.getV();
        final float width = tileWidth / texture.getWidth();
        final float height = tileHeight / texture.getHeight();

        final float insetX = 0.05f / texture.getWidth();
        final float insetY = 0.05f / texture.getHeight();
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) {
                final float u = tileSetU + i * width + insetX;
                final float v = tileSetV + j * height + insetY;
                this.tileSet[i + j * 4] = new TextureRegion(texture, u, v, u + width - 2 * insetX, v + height - 2 * insetY);
            }

        reset(false);
    }

    public void reset(final boolean state) {
        for (final boolean[] row : tiles)
            Arrays.fill(row, state);
        final byte tile = conversions.get(state ? 0b1111 : 0b0000);
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
            indices[nX][nY] = conversions.get(bitmask);
        }
    }

    public void render(final Batch batch) {
        overlay.bind(1);
        texture.bind(0);

        shader.bind();
        shader.setUniformi("u_overlay", 1);
        shader.setUniformi("u_texture", 0);
        shader.setUniformf("u_scale", overlayScale / unitScale);

        batch.setShader(shader);
        for (int x = 0; x < tilesX; x++)
            for (int y = 0; y < tilesY; y++)
                batch.draw(tileSet[indices[x][y]], (offsetX + x * tileWidth) * unitScale, (offsetY + y * tileHeight) * unitScale, tileWidth * unitScale, tileHeight * unitScale);
        batch.setShader(null);
    }

}
