#!/usr/bin/env python3
import sys
from PIL import Image

def upscale_icon(input_path, output_path, size=(256, 256)):
    try:
        # Open the original image
        img = Image.open(input_path)
        
        # Upscale using LANCZOS resampling for better quality
        upscaled = img.resize(size, Image.Resampling.LANCZOS)
        
        # Save the upscaled image
        upscaled.save(output_path, "PNG")
        print(f"Successfully upscaled {input_path} to {output_path} ({size[0]}x{size[1]})")
        return True
    except Exception as e:
        print(f"Error: {e}")
        return False

if __name__ == "__main__":
    input_file = "icon.png"
    output_file = "icon_256.png"
    
    if len(sys.argv) > 1:
        input_file = sys.argv[1]
    if len(sys.argv) > 2:
        output_file = sys.argv[2]
    
    success = upscale_icon(input_file, output_file)
    sys.exit(0 if success else 1)
