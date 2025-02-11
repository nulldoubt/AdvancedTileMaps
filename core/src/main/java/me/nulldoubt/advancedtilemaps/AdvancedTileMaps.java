package me.nulldoubt.advancedtilemaps;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class AdvancedTileMaps extends ApplicationAdapter {

    private static final Vector2 temp = new Vector2();

    private Texture dirt;
    private Texture dirtOverlay;
    private TileLayer dirtLayer;

    private Texture grass;
    private Texture grassOverlay;
    private TileLayer grassLayer;

    private Batch batch;
    private ShaderProgram shader;
    private FitViewport viewport;

    @Override
    public void create() {
        batch = new SpriteBatch();
        dirt = new Texture("Dirt.png");
        dirtOverlay = new Texture("DirtOverlay.png");
        dirtOverlay.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        dirtOverlay.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        grass = new Texture("Grass.png");
        grassOverlay = new Texture("GrassOverlay.png");
        grassOverlay.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        grassOverlay.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        shader = new ShaderProgram(
            Gdx.files.internal("shaders/overlay.vert").readString(),
            Gdx.files.internal("shaders/overlay.frag").readString()
        );
        if (!shader.isCompiled())
            throw new RuntimeException("Unable to compile shader: " + shader.getLog());

        batch = new SpriteBatch();
        viewport = new FitViewport(16f, 9f);

        dirtLayer = new TileLayer(new TextureRegion(dirt), dirtOverlay, shader, 64, 64, 16f, 16f, 1f / 16f);
        dirtLayer.reset(true);

        grassLayer = new TileLayer(new TextureRegion(grass), grassOverlay, shader, 64, 64, 16f, 16f, 1f / 16f);
        grassLayer.reset(false);

        Gdx.input.setInputProcessor(new InputAdapter() {

            @Override
            public boolean keyDown(final int keycode) {
                switch (keycode) {
                    case Input.Keys.W -> viewport.getCamera().position.y++;
                    case Input.Keys.A -> viewport.getCamera().position.x--;
                    case Input.Keys.S -> viewport.getCamera().position.y--;
                    case Input.Keys.D -> viewport.getCamera().position.x++;
                    default -> {}
                }
                return true;
            }

            @Override
            public boolean touchDragged(final int screenX, final int screenY, final int pointer) {
                final Vector2 touch = viewport.unproject(temp.set(screenX, screenY));
                grassLayer.tileAt(
                    (int) (touch.x - 1f),
                    (int) (touch.y - 1f),
                    Gdx.input.isButtonPressed(Input.Buttons.LEFT)
                );
                return true;
            }

            @Override
            public boolean touchDown(final int screenX, final int screenY, final int pointer, final int button) {
                final Vector2 touch = viewport.unproject(temp.set(screenX, screenY));
                grassLayer.tileAt(
                    (int) (touch.x - 1f),
                    (int) (touch.y - 1f),
                    button == Input.Buttons.LEFT
                );
                return true;
            }

        });
    }

    @Override
    public void render() {
        ScreenUtils.clear(Color.BLACK);

        final OrthographicCamera camera = (OrthographicCamera) viewport.getCamera();
        final float width = (viewport.getWorldWidth() / 2f);
        final float height = (viewport.getWorldHeight() / 2f);

        camera.position.set(
            MathUtils.clamp(camera.position.x, width + 1f, dirtLayer.tilesX * dirtLayer.tileWidth * dirtLayer.unitScale - width),
            MathUtils.clamp(camera.position.y, height + 1f, dirtLayer.tilesY * dirtLayer.tileHeight * dirtLayer.unitScale - height),
            0f
        );
        viewport.apply();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        dirtLayer.render(batch);
        grassLayer.render(batch);

        batch.end();
    }

    @Override
    public void resize(final int width, final int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {

        dirt.dispose();
        dirtOverlay.dispose();

        grass.dispose();
        grassOverlay.dispose();

        shader.dispose();
        batch.dispose();
    }

}
