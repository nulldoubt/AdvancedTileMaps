Advanced Tilemaps
---
Ever wondered how you can create awesome, overlayed, auto-tiled, 
multi-layered, viewport-managed, and efficient tilemaps in libGDX 
using the **dual grid** system?

What's the dual-grid system?
---
The dual-grid system is a basically a mechanism similar to 
the marching squares algorithm, it allows to test the tile 
corners using a 4-bit long bitmask, this allows you to automatically 
set the tile texture based on the 4 corners. Why's that ideal? 
Well, now you don't have to draw 256 different tile combinations 
to display a nice tilemap with round edges and corners, that's only 
if you're testing the tile edges. Instead, you only have to 
provide a tile-set consisting of 16 different tile combinations 
which is just suitable for testing using the tile corners. That's 
a huge amount of work spared for a simple tile layer!

![Window-Preview.png](media/Window-Preview.png)

The first time I saw the dual-grid system was in an awesome 
dev-log by [jess::codes](https://www.youtube.com/@jesscodes), 
she's really an awesome and talented game developer!

The mechanisms implemented in this simple game were basically 
inspired these two videos from her:
* [Draw fewer tiles - by using a Dual-Grid system!](https://www.youtube.com/watch?v=jEWFSv3ivTg)
* [Easy texture overlay shader for tilemaps!](https://www.youtube.com/watch?v=eYlBociPwdw)

Also, just as a side note, the assets used in this repository 
are copied from her demos: 
* [dual-grid-tilemap-system-godot](https://github.com/jess-hammer/dual-grid-tilemap-system-godot)
* [repeated-texture-on-tilemap-demo-godot](https://github.com/jess-hammer/repeated-texture-on-tilemap-demo-godot)

Thanks [jess](https://github.com/jess-hammer)!

What does 'overlayed' mean?
---
Every `TileLayer` uses a [custom shader](/assets/shaders) to render 
an overlay texture on top of the color mask of the tile-set (which 
is red by default). This you to have a non-repetitive and seamless 
texture rendered on your layers, it also enables you to draw art 
that's bigger than just a single tile in size.

What does 'auto-tiled' mean?
---
Every `TileLayer` utilizes a mechanism to bitmask every tile and 
change the tile-set index of that tile based on the 4 corners and 
their surrounding neighbors. This enables the game to automatically 
display the right tile texture based on the neighbors and that's 
called auto-tiling.

![Auto-Tiling.gif](media/Auto-Tiling.gif)

And what does 'multi-layered' mean?
---
The approach here is to implement the logic into separate instances 
of `TileLayer` and then render them in the order you'd like, this allows 
you to auto-tile multiple tile types in the same map.

Finally, what does 'viewport-managed' mean?
---
The game is viewport managed, that means that you don't have to 
use ANY pixels in your game. The only units you work with are 
the so-called world units. And you define them!

A simple example: Every tile in this game is, actually, **1x1 
world units** in size. That's ideal because this approach 
scales well when you're building your game for other platforms 
with different screen sizes and resolutions.

Maybe a small diagram can help you understand [the code](example/src/main/java/me/nulldoubt/advancedtilemaps/example/AdvancedTileMaps.java#L121-L122)?

![Viewport-Diagram.png](media/Viewport-Diagram.png)

The viewport in our case is basically just what defines 
the part that we want to *see* from the world. But not, 
actually, the camera is what we see. When you set the 
camera zoom to 1.0, then you see the whole viewport. 
*And don't worry, your world may be bigger than the viewport!*

How to run the example?
---
It's really simple, just clone the repository and run 
it using the gradle-wrapper. Using this command:
```
./gradlew clean :example:run
```

How to build the library?
---
Building is simple as well, clone the repository and 
build the jar file using the gradle-wrapper. Using this command: 
```
./gradlew clean :library:jar
```

Or even better, this *library* is literally [a single class](library/src/main/java/me/nulldoubt/advancedtilemaps/TileLayer.java), 
just copy it to your project and done!

Future?
---
I will try my best to maintain this library and fix future 
bugs, maybe even implement features and optimize it. Pull 
requests are very welcome.

A simple note: As of now, there are many simple, possible 
optimizations for the `TileLayer` class. Keep in mind that 
this is the first draft of the library, so you may find it 
unoptimized at first.

License?
---
This library (not the example assets!) is public domain.
By library, I mean everything within the [library](library) folder.
