# Vision Regression Checklist

Use this quick manual check after changing AuraChat's image pipeline, Gemini prompt setup, or attachment UI.

## Core case

1. Open chat and attach exactly one photo.
2. Send the prompt: `How many photos did I send?`
3. Expected result: the model answers `1` photo, `1 image`, or equivalent wording.
4. Failure to watch for: the model says the single image was multiple photos, crops, tiles, or screenshots.

## Follow-up case

1. Attach one photo with clear subject detail.
2. Send: `Describe this image in one sentence.`
3. Expected result: the answer describes the subject, not internal crops or image-processing steps.

## Notes

- AuraChat intentionally stores the original attachment for UI display, but sends a downscaled bitmap to Gemini for vision analysis.
- The active system instruction also tells Gemini not to count internal crops or tiles as separate photos.
