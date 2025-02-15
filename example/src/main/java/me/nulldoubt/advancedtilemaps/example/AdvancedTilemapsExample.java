package me.nulldoubt.advancedtilemaps.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.*;
import me.nulldoubt.advancedtilemaps.TileLayer;

public class AdvancedTilemapsExample extends ApplicationAdapter {

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired())
            return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new AdvancedTilemapsExample(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Advanced Tilemaps Example");
        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        configuration.setWindowedMode(800, 450);
        return configuration;
    }

    private static final Vector2 temp = new Vector2();
    private static final String interfaceDebugInfo = """
        Rendered Tiles [CYAN](Dirt): %d[]
        Rendered Quads [CYAN](Dirt): %d[]

        Rendered Tiles [GREEN](Grass): %d[]
        Rendered Quads [GREEN](Grass): %d[]

        Camera [YELLOW]Position: (%.2f, %.2f)[]
        Camera [YELLOW]Zoom: %.4f[]

        FPS: [MAGENTA]%d[]
        Draw-Calls: [MAGENTA]%d[]
        """;

    // These here are the variables for our 'dirt' tile layer.
    private Texture dirt;
    private Texture dirtOverlay;
    private TileLayer dirtLayer;

    // and these here are the variables for our 'grass' tile layer.
    private Texture grass;
    private Texture grassOverlay;
    private TileLayer grassLayer;

    private FileHandle grassLayerFile;

    /*
        and these here are the necessary variables
        for us to draw the tile layers.
    */
    private SpriteBatch batch;
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
    private Viewport worldViewport;
    private OrthographicCamera worldCamera;

    private Viewport interfaceViewport;
    private OrthographicCamera interfaceCamera;

    private BitmapFont font;
    private BitmapFontCache fontCache;

    private int drawCalls;

    private final Vector2 cameraVelocity = new Vector2();
    private final float cameraSpeed = 21f; // in world-units-per-second.

    private float targetZoom;
    private float zoomSpeed = 21f;

    private boolean _touchDown;
    private boolean _buttonRight;

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
        batch = new SpriteBatch(3000);

        // creating our viewport, with our world bounds being of the same aspect-ratio of our native resolution.
        worldViewport = new FitViewport(40f, 22.5f);
        worldCamera = (OrthographicCamera) worldViewport.getCamera();
        worldCamera.zoom = targetZoom = 1f / 2.5f;

        interfaceViewport = new ScreenViewport();
        interfaceCamera = (OrthographicCamera) interfaceViewport.getCamera();

        // initializing our dirt tile layer.
        dirtLayer = new TileLayer(64, 64, 16f, 16f, 1f / 16f, true);
        dirtLayer.setTileSet(new TextureRegion(dirt));
        dirtLayer.setOverlay(dirtOverlay, shader);

        // initializing our grass tile layer.
        grassLayerFile = Gdx.files.local("grass_layer.bin");
        if (grassLayerFile.exists())
            grassLayer = TileLayer.read(grassLayerFile);
        else
            grassLayer = new TileLayer(64, 64, 16f, 16f, 1f / 16f, false);
        grassLayer.setTileSet(new TextureRegion(grass));
        grassLayer.setOverlay(grassOverlay, shader);

        // setting the default input processor.
        Gdx.input.setInputProcessor(new InputAdapter() {

            @Override
            public boolean scrolled(float amountX, float amountY) {
                targetZoom += (amountY * 0.25f);
                targetZoom = MathUtils.clamp(targetZoom, 0.1f, 1.5f);
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (!_touchDown)
                    return false;
                handleTile(screenX, screenY);
                return true;
            }

            @Override
            public boolean touchDown(final int screenX, final int screenY, final int pointer, final int button) {
                _buttonRight = (button == Input.Buttons.RIGHT);
                _touchDown = true;
                handleTile(screenX, screenY);
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                _touchDown = false;
                return true;
            }
        });

        font = new BitmapFont();
        font.getData().markupEnabled = true;
        fontCache = font.newFontCache();
    }

    private void handleTile(int screenX, int screenY) {
        final Vector2 touch = worldViewport.unproject(temp.set(screenX, screenY));
        grassLayer.tileAt(
            (int) (touch.x - 1f),
            (int) (touch.y - 1f),
            !_buttonRight
        );
    }

    @Override
    public void render() {
        drawCalls = batch.renderCalls;
        ScreenUtils.clear(Color.BLACK); // clear the screen.

        final float delta = Gdx.graphics.getDeltaTime();

        if (targetZoom != worldCamera.zoom)
            worldCamera.zoom = MathUtils.lerp(worldCamera.zoom, targetZoom, delta * zoomSpeed);

        final float width = (worldViewport.getWorldWidth() / 2f) * worldCamera.zoom;
        final float height = (worldViewport.getWorldHeight() / 2f) * worldCamera.zoom;

        if (Gdx.input.isKeyPressed(Input.Keys.W))
            cameraVelocity.y++;
        if (Gdx.input.isKeyPressed(Input.Keys.A))
            cameraVelocity.x--;
        if (Gdx.input.isKeyPressed(Input.Keys.S))
            cameraVelocity.y--;
        if (Gdx.input.isKeyPressed(Input.Keys.D))
            cameraVelocity.x++;
        float length = cameraVelocity.len();
        if (length > 1f)
            cameraVelocity.nor();

        worldCamera.position.x += cameraVelocity.x * cameraSpeed * delta;
        worldCamera.position.y += cameraVelocity.y * cameraSpeed * delta;
        cameraVelocity.setZero();

        // here, we clamp the camera position to our world boundaries.
        worldCamera.position.set(
            MathUtils.clamp(worldCamera.position.x, width + 1f, dirtLayer.getTilesX() * dirtLayer.getTileWidth() * dirtLayer.getUnitScale() - width),
            MathUtils.clamp(worldCamera.position.y, height + 1f, dirtLayer.getTilesY() * dirtLayer.getTileHeight() * dirtLayer.getUnitScale() - height),
            0f
        );

        /*
            URGENT: if you have a viewport or configured the tile
            layers to use a unit-scale, then apply your viewport here!
         */
        worldViewport.apply();

        // When the camera moves, and we're dragging, then tiles should be set!
        if (length > 0.01f && _touchDown)
            handleTile(Gdx.input.getX(), Gdx.input.getY());

        // setting the viewport camera projection matrix.
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin(); // begin the batch.
        renderWorld(batch);
        renderInterface(batch);
        batch.end(); // end the batch.
    }

    private void renderWorld(Batch batch) {
        /*
            rendering the layers, warning: the order of
            rendering plays a huge role, and it's up to
            you to manage it properly.
         */
        dirtLayer.setView(worldCamera);
        dirtLayer.render(batch); // first comes the dirt.

        grassLayer.setView(worldCamera);
        grassLayer.render(batch); // then comes the grass.
    }

    private void renderInterface(Batch batch) {
        interfaceViewport.apply(true);
        batch.setProjectionMatrix(interfaceCamera.combined);
        fontCache.clear();
        fontCache.addText(String.format(
            interfaceDebugInfo,
            dirtLayer.getTilesRendered(),
            dirtLayer.getQuadsRendered(),
            grassLayer.getTilesRendered(),
            grassLayer.getQuadsRendered(),
            worldCamera.position.x,
            worldCamera.position.y,
            worldCamera.zoom,
            Gdx.graphics.getFramesPerSecond(),
            drawCalls
        ), 15f, Gdx.graphics.getHeight() - 20f);
        fontCache.draw(batch);
    }

    @Override
    public void resize(final int width, final int height) {
        /*
            when the window resizes, we'd like our viewport
            to update the internal screen size to properly
            handle the conversion of our world units.
         */
        worldViewport.update(width, height, true);
        interfaceViewport.update(width, height, false);
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

        font.dispose();

        TileLayer.write(grassLayer, grassLayerFile);
    }

}
