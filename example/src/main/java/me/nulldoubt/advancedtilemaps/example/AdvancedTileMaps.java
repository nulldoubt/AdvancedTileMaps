package me.nulldoubt.advancedtilemaps.example;

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
import com.badlogic.gdx.utils.viewport.*;
import me.nulldoubt.advancedtilemaps.TileLayer;

public class AdvancedTileMaps extends ApplicationAdapter {

    private static final Vector2 temp = new Vector2();

    // These here are the variables for our 'dirt' tile layer.
    private Texture dirt;
    private Texture dirtOverlay;
    private TileLayer dirtLayer;

    // and these here are the variables for our 'grass' tile layer.
    private Texture grass;
    private Texture grassOverlay;
    private TileLayer grassLayer;

    /*
        and these here are the necessary variables
        for us to draw the tile layers.
    */
    private Batch batch;
    private ShaderProgram shader;

    /*

        and finally, this is a viewport that's optional
        but really recommended. It manages both our camera
        and the gl-viewport state.

        It allows us draw in our own world units and
        provides better compatibility for other screen
        resolutions.

        We constructed a FitViewport, which fixes the
        aspect ratio of your defined world coordinates
        and causes black borders to appear to keep that
        aspect ratio.

        So, let's say you usually have a 16x16 tile and
        in all of your draw-calls, you use

            batch.draw(tile, x, y, 16f, 16f); -> tile is 16x16 pixels, depending on screen resolution.

        that will hardly scale up if you build a nice
        game, and it will be even harder if you let
        your physics library use the pixels instead
        of your actual world-bounds.

        So, *my* recommended approach is to actually
        create a fit-viewport with your desired world
        bounds. The world bounds should have the same
        aspect as your native resolution for the game.

        Say, if your native resolution is 800x450, your
        aspect ratio is 16:9 (e.g. 450 / 800 = 1.78 = 16:9)
        and your fit-viewport should be of that aspect
        ratio, I simply chose the size (16, 9) here.

        Now, before rendering, you have to apply your
        viewport, and you can use your world units
        without ever worrying:

            batch.draw(tile, x, y, 1f, 1f); -> tile is 1x1 world units, independent of screen resolution.

     */
    private Viewport viewport;

    @Override
    public void create() {

        // initializing the dirt tile layer members.
        dirt = new Texture("Dirt.png");
        dirtOverlay = new Texture("DirtOverlay.png");

        // dirt tile layer is pixel art, we don't want it to be blurry.
        dirtOverlay.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // URGENT: this here is essential for the overlay tile maps to work properly.
        dirtOverlay.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        // initializing the grass tile layer members.
        grass = new Texture("Grass.png");
        grassOverlay = new Texture("GrassOverlay.png");

        // grass tile layer is pixel art as well, and we don't want it to be blurry either.
        grassOverlay.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // URGENT: this here is essential for the overlay tile maps to work properly.
        grassOverlay.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        // creating our shader program.
        shader = new ShaderProgram(
            Gdx.files.internal("shaders/overlay.vert").readString(),
            Gdx.files.internal("shaders/overlay.frag").readString()
        );

        // a post-compilation check to see if our shader program compiled successfully.
        if (!shader.isCompiled())
            throw new RuntimeException("Unable to compile shader: " + shader.getLog());

        // creating our sprite-batch.
        batch = new SpriteBatch();

        // creating our viewport, with our world bounds being of the same aspect-ratio of our native resolution.
        viewport = new FitViewport(40f, 22.5f);
        ((OrthographicCamera) viewport.getCamera()).zoom = 1f / 2.5f;

        // initializing our dirt tile layer.
        dirtLayer = new TileLayer(64, 64, 16f, 16f, 1f / 16f, true);
        dirtLayer.setTileSet(new TextureRegion(dirt));
        dirtLayer.setOverlay(dirtOverlay, shader);

        // initializing our grass tile layer.
        grassLayer = new TileLayer(64, 64, 16f, 16f, 1f / 16f, false);
        grassLayer.setTileSet(new TextureRegion(grass));
        grassLayer.setOverlay(grassOverlay, shader);

        // setting the default input processor.
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
        ScreenUtils.clear(Color.BLACK); // clear the screen.

        final OrthographicCamera camera = (OrthographicCamera) viewport.getCamera();
        final float width = (viewport.getWorldWidth() / 2f);
        final float height = (viewport.getWorldHeight() / 2f);

        // here, we clamp the camera position to our world boundaries.
        camera.position.set(
            MathUtils.clamp(camera.position.x, width + 1f, dirtLayer.getTilesX() * dirtLayer.getTileWidth() * dirtLayer.getUnitScale() - width),
            MathUtils.clamp(camera.position.y, height + 1f, dirtLayer.getTilesY() * dirtLayer.getTileHeight() * dirtLayer.getUnitScale() - height),
            0f
        );

        /*
            URGENT: if you have a viewport or configured the tile
            layers to use a unit-scale, then apply your viewport here!
         */
        viewport.apply();

        // setting the viewport camera projection matrix.
        batch.setProjectionMatrix(camera.combined);
        batch.begin(); // begin the batch.

        /*
            rendering the layers, warning: the order of
            rendering plays a huge role, and it's up to
            you to manage it properly.
         */
        dirtLayer.setView(camera);
        dirtLayer.render(batch); // first comes the dirt.

        grassLayer.setView(camera);
        grassLayer.render(batch); // then comes the grass.

        batch.end(); // end the batch.

        // We'd like to know how many tiles we rendered!
        System.out.printf("\rDirt tiles rendered: %03d", dirtLayer.getTilesRendered());
    }

    @Override
    public void resize(final int width, final int height) {
        /*
            when the window resizes, we'd like our viewport
            to update the internal screen size to properly
            handle the conversion of our world units.
         */
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {

        // and here, we dispose everything!

        dirt.dispose();
        dirtOverlay.dispose();

        grass.dispose();
        grassOverlay.dispose();

        shader.dispose();
        batch.dispose();

    }

}
