#!/usr/bin/python

import math
import time
from hal import Hal
h = Hal()

def run():
    while not h.touchsensors("Head", "Front"):
        h.rasta(1000)

def main():
    try:
        run()
    except Exception as e:
        h.say("Error!")

if __name__ == "__main__":
    main()