#!/usr/bin/python

import math
import time
from hal import Hal
h = Hal()

def run():
    h.stiffness("Body", 2)

def main():
    try:
        run()
    except Exception as e:
        h.say("Error!")

if __name__ == "__main__":
    main()