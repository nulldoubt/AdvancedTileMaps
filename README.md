# Advanced Tilemaps

Ever wondered how you can create **overlayed, auto-tileable, multi-layered, viewport-friendly, serializable, performant,
and customizable** tilemaps in libGDX with **minimal effort** using the **dual-grid system**?

## What is the Dual-Grid System?

The dual-grid system is a mechanism similar to the **marching squares algorithm**. It allows tile textures to be
displayed based on their **four cornering neighbors**, using a **4-bit bitmask**.

### Why is this ideal?

With traditional auto-tiling, you’d need **256 different tile combinations** to achieve smooth edges and corners using
an **8-neighbor system**. However, with the dual-grid approach, you only need **16 tile combinations**, significantly
reducing the amount of artwork required while still achieving a polished result.

![Window-Preview.png](media/Window-Preview.png)

The first time I saw the dual-grid system was in an awesome dev-log
by [jess::codes](https://www.youtube.com/@jesscodes). She is an incredibly talented game developer!

This library was largely inspired by these two videos:

- [Draw fewer tiles - by using a Dual-Grid system!](https://www.youtube.com/watch?v=jEWFSv3ivTg)
- [Easy texture overlay shader for tilemaps!](https://www.youtube.com/watch?v=eYlBociPwdw)

Additionally, most assets in this repository are taken from her demos:

- [dual-grid-tilemap-system-godot](https://github.com/jess-hammer/dual-grid-tilemap-system-godot)
- [repeated-texture-on-tilemap-demo-godot](https://github.com/jess-hammer/repeated-texture-on-tilemap-demo-godot)

Thanks [jess](https://github.com/jess-hammer)!

## Feature Breakdown

### What does "overlayed" mean?

Each `TileLayer` can have an optional **overlay shader** to render an **overlay texture** on top of the **color mask**
of the tile-set (default: red). This helps prevent repetition and allows drawing larger details that span multiple
tiles.

### What does "auto-tileable" mean?

Each `TileLayer` uses a bitmasking system to **automatically determine** the correct tile variant based on its **four
cornering neighbors**. This means you **don’t have to manually place tiles**—the system will handle it for you!

**Here's a little showcase of the example included (press to play the video):**

![Showcase.mp4](media/Showcase.mp4

### What does "multi-layered" mean?

The library allows you to **stack multiple tile layers**, enabling independent auto-tiling for different tile types
within the same map.

### What does "viewport-friendly" mean?

The example is managed using a **viewport system**, meaning that you work with **world units** instead of pixels. This
ensures **scalability** across different screen sizes and resolutions.

For example, each tile in the example is **1x1 world units**, making the system highly adaptable.

![Viewport-Diagram.png](media/Viewport-Diagram.png)

A viewport defines and manages the coordinate system of your game world, making your game more portable across
platforms.

Every viewport manages a camera which may be freely positioned within the world,
the camera is often what's visible on the screen.

## How to Use the Library

This library consists of a single, configurable class: `TileLayer`.

### Creating a Tile Layer

You can create a new `TileLayer` instance using:

```java
new TileLayer(
        int tilesX,         // World width in tiles
        int tilesY,         // World height in tiles
        float tileWidth,    // Tile width
        float tileHeight,   // Tile height
        float unitScale,    // Unit scale of the world
        boolean fill        // Should the layer start filled?
);
```

### Setting Up the Tile Set

Once you have an instance, set the tile texture:

```java
tileLayer.setTileSet(textureRegion);
```

### Adding an Overlay (Optional)

You can integrate an **overlay texture** and an **overlay shader** using:

```java
tileLayer.setOverlay(overlayTexture, overlayShader);
```

### Rendering the Tile Layer

In your render pipeline:

```java
tileLayer.setView(camera); // Set view bounds to the camera
tileLayer.render(batch);   // Render using a batch
```

*You may also use the overloaded method `setView(x, y, w, h)` if you don't have a camera.*

### Handling Texture Bleeding

If you experience **texture bleeding**, adjust the inset tolerance:

```java
TileLayer.setInsetTolerance(float xTolerance, float yTolerance);
```

This allows you to handle texture bleeding properly at runtime, without ever modifying your texture.

Suggested values:

- `0.001` (for large textures, e.g., `>= 4096px`)
- `0.05` (for small textures, e.g., `<= 16px`)

### Custom Auto-Tile Configuration

If your tile-set layout differs from the default, you can set a **custom auto-tile configuration**:

```java
TileLayer.setAutoTileConfiguration(IntMap<Byte> configuration);
```

*Note that this is currently a **static property**, meaning it applies to all tile layers.*

### Rendering Strategies

You may experiment with different `RenderStrategy` for your tilemap, there are 4 rendering strategies
integrated as of now:

* `IntegratedStrategy.ALL_TILES_ALL_QUADS` will render all tiles and all quads.
* `IntegratedStrategy.ALL_TILES_VIEW_QUADS` will render all tiles but only visible quads.
* `IntegratedStrategy.VIEW_TILES_ALL_QUADS` will render visible tiles but all quads.
* (default) `IntegratedStrategy.VIEW_TILES_VIEW_QUADS` will render visible tiles and only visible quads.

*Invisible quads are the ones associated with bitmask 0 in the auto-tile configuration.*

**You can also provide your own implementation of the `RenderStrategy` interface.**

You may change the current tile layer rendering strategy like this:

```java
tileLayer.setRenderStrategy(renderStrategy);
```

### Serialization

In case you want to serialize your tile layers, the `TileLayer` class offers a couple of
convenient and efficient static methods that simplify the serialization process for you
using the `UBJson` file format.

You may *write* your tile layer to a file handle or an output stream like this:

```java
TileLayer.write(tileLayer, fileHandle); // write to a file handle.
TileLayer.write(tileLayer, outputStream); // write to an output stream.
```

And you may *read* your tile layer from a file handle or an input stream like this:

```java
TileLayer.read(fileHandle); // read from a file handle.
TileLayer.read(outputStream); // read from an input stream.
```

## Library vs. Example

This repository contains both the **library** and an **example project**:

- **Library (`:library` Gradle submodule)**
    - Contains everything related to the `TileLayer` class.
    - Located in the [`library`](library) folder.
    - This is the core functionality meant for integration into your own projects.

- **Example (`:example` Gradle submodule)**
    - A demonstration of how to use `TileLayer`.
    - Located in the [`example`](example) folder.
    - Provides a working implementation showcasing auto-tiling, overlays, and viewport management.

## Running the Example

Clone the repository and run:

```shell
./gradlew clean :example:run
```

## Building the Library

To build the library JAR file:

```shell
./gradlew clean :library:jar
```

Alternatively, since the **entire library is a single class**, you can simply copy [
`TileLayer.java`](library/src/main/java/me/nulldoubt/advancedtilemaps/TileLayer.java) into your project.

## Future Plans

I will do my best to maintain this library, fix bugs, and possibly add new features and optimizations.

Pull requests are **highly welcome!**

Currently, `TileLayer` is well-optimized and can efficiently handle **large worlds**.

## License

This library (excluding example assets) is **public domain**.

Everything inside the [`library`](library) folder is **free to use** without restrictions.

