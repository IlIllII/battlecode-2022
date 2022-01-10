import sys

# arg1 = sys.argv
# arg = arg1[1]



for arg in [70,72,96,98,58,82,86,110,57,59,69,73,95,99,109,111,56,60,108,112,145,81,87,123,44,46,68,74,94,100,122,124,43,47,55,61,107,113,121,125,132,80,88,136,31,33,67,75,93,101,135,137,42,48,120,126,30,34,54,62,106,114,134,138]:
        
    print() 

    print(f"if (rc.isLocationOccupied(l{arg})) {{\np{arg} = 1000;\n}} else {{\np{arg} = rc.senseRubble(l{arg}) / 10;\n}}")

    print()

# for i in range(69):
#     print(f"static MapLocation t{i};\nstatic Direction d{i};\nstatic double v{i};\nstatic double p{i};")