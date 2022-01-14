# player26

In player23 I noticed that targeting was using at times 6000+ bytecode and pathfinding used 6300 bytecode, meaning during complex targeting situations our units would fall back to more basic movement which is not ideal. I started to optimize targeting to use less bytecode but then decided to make a new player for regression testing purposes while I tweak things.

In player26 I will implement a few move strategies to reduce pathfinding costs as well as decrease targeting costs without sacrificing intelligent unit navigation. If these are effective player26 should be able to beat player23 without further tuning.


Effective movement optimizations:

1. Removing `isLocationOccupied` conditionals from all tiles except adjacent tiles. 6300 -> 4300
2. Removing the rear 1/6th of the tiles to BFS and only switching on the wedge in front of us. 4300 -> 3800
3. Making north point to target allows you to get rid of half of the distance checks. 3800 -> 3500
4. BFSing only a half circle in front of us. 3500 -> 3000
5. Only initializing tiles that we use and directions. 3000 -> 2450



## Common costs:

These are rough costs. For instance, if statemets cost 1, == costs 1, and literal/variable accesses cost 1. So a statement such as:

```java
if (x == 1) {

}
```

costs 4 bytecode.

| Operation | Cost |
| --- | --- |
| if statement overhead | 1 |
| = | 1 |
| == | 1 |
| accessing literals/variables | 1 |
| dereferencing pointers | 2 |
| accessing array values | 2 |
| + | 1 |
| / | 1 |
| * | 1 |


int vs double makes no difference.