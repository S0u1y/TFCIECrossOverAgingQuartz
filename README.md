## What it do:

This mod is aimed to fix the fact that TFC+IEC's quartz in the overworld is implemented as a vanilla amethys. The player would have to constantly stay in the same chunk of the geode and chunkload it for at least 90 minutes on average to (hopefully) grow fully.

But with this mod, you can go back to your base and go about your TFC progression! Just come back after some time has passed and you'll see **the budding quartz blocks were still growing while you were gone!**

## How it works:

The mod creates a lastUpdate timestamp on chunks that get unloaded and when they are loaded again, it checks for all budding quartz blocks and uses poisson probabilistic calculation to simulate growth for each of those budding blocks.

## Future updates:

- Add config and functionality for first time world gen.
- Add catch-up aging for beach salts.

Logo image was generated with Gemini & GPT (I'm no artist, sorry!).
