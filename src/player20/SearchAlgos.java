package player20;

import java.util.ArrayList;

import battlecode.common.*;

class NodeHeap {
    private BFSNode[] Heap;
    private int size;
    private int maxSize;
    private RobotController rc;

    private static final int FRONT = 1;

    public NodeHeap(int maxSize, RobotController rc) {
        this.maxSize = maxSize;
        this.size = 0;
        this.rc = rc;

        Heap = new BFSNode[this.maxSize + 1];
        Heap[0] = new BFSNode(new MapLocation(1000, 1000), null, -1000000, new MapLocation(1000, 1000));
    }

    private int parent(int pos) {
        return (pos / 2);
    }

    private int leftChild(int pos) {
        return (2 * pos);
    }

    private int rightChild(int pos) {
        return (2 * pos) + 1;
    }

    private boolean isLeaf(int pos) {
        if (pos > (size / 2) && pos <= size) {
            return true;
        } 
        return false;
    }

    private void swap(int pos1, int pos2) {
        BFSNode tmp;
        tmp = Heap[pos1];
        Heap[pos1] = Heap[pos2];
        Heap[pos2] = tmp;
    }

    private void minHeapify(int pos) {
        if (!isLeaf(pos)) {
            if (Heap[pos].greaterThan(Heap[leftChild(pos)])
            || Heap[pos].greaterThan(Heap[rightChild(pos)])) {

                if (Heap[leftChild(pos)].lessThan(Heap[rightChild(pos)])) {
                    swap(pos, leftChild(pos));
                    minHeapify(leftChild(pos));
                } else {
                    swap(pos, rightChild(pos));
                    minHeapify(rightChild(pos));
                }
            } 
        }
    }

    public void insert(BFSNode node) {
        if (size >= maxSize) {
            return;
        }
        Heap[++size] = node;
        int current = size;

        while (Heap[parent(current)].greaterThan(Heap[current]) && current != 0) {
            swap(current, parent(current));
            current = parent(current);
        }
    }

    public void print() {
        System.out.println("Heap size: " + size);
        for (int i = 1; i <= size / 2; i++) {
            System.out.print(" PARENT : " + Heap[i].toString() + " LEFT CHILD : " + Heap[2 * i] + " RIGHT CHILD : " + Heap[2 * i + 1]);
            System.out.println();
        }
    }

    public BFSNode remove() {
        // if (size - 1 < 0) {
        //     System.out.println("Tried to remove from an empty heap");
        // }
        BFSNode popped = Heap[FRONT];
        Heap[FRONT] = Heap[size--];
        if (size > 0) {
            minHeapify(FRONT);
        }

        return popped;
    }

    public boolean isEmpty() {
        return size <= 0;
    }

    public void printSzie() {
        System.out.println("Heap size: " + size);
    }

    public int length() {
        return size;
    }
}


class HashNode {
    MapLocation key;
    BFSNode value;
    final int hashCode;

    HashNode next;

    public HashNode(MapLocation key, BFSNode value, int hashCode) {
        this.key = key;
        this.value = value;
        this.hashCode = hashCode;
    }
}

class Map {
    private ArrayList<HashNode> bucketArray;
 
    // Current capacity of array list
    private int numBuckets;
 
    // Current size of array list
    private int size;
 
    // Constructor (Initializes capacity, size and
    // empty chains.
    public Map()
    {
        bucketArray = new ArrayList<>();
        numBuckets = 10;
        size = 0;
 
        // Create empty chains
        for (int i = 0; i < numBuckets; i++)
            bucketArray.add(null);
    }
 
    public int size() { return size; }
    public boolean isEmpty() { return size() == 0; }

   
    // This implements hash function to find index
    // for a key
    private int getBucketIndex(MapLocation key)
    {
        int index = key.hashCode() % numBuckets;
        // key.hashCode() coule be negative.
        index = index < 0 ? index * -1 : index;
        return index;
    }
 
    // Method to remove a given key
    public BFSNode remove(MapLocation key)
    {
        // Apply hash function to find index for given key
        int bucketIndex = getBucketIndex(key);
        int hashCode = key.hashCode();
        // Get head of chain
        HashNode head = bucketArray.get(bucketIndex);
 
        // Search for key in its chain
        HashNode prev = null;
        while (head != null) {
            // If Key found
            if (head.key.equals(key) && hashCode == head.hashCode)
                break;
 
            // Else keep moving in chain
            prev = head;
            head = head.next;
        }
 
        // If key was not there
        if (head == null)
            return null;
 
        // Reduce size
        size--;
 
        // Remove key
        if (prev != null)
            prev.next = head.next;
        else
            bucketArray.set(bucketIndex, head.next);
 
        return head.value;
    }
 
    // Returns value for a key
    public BFSNode get(MapLocation key)
    {
        // Find head of chain for given key
        int bucketIndex = getBucketIndex(key);
        int hashCode = key.hashCode();
       
        HashNode head = bucketArray.get(bucketIndex);
 
        // Search key in chain
        while (head != null) {
            if (head.key.equals(key) && head.hashCode == hashCode)
                return head.value;
            head = head.next;
        }
 
        // If key not found
        return null;
    }

    public boolean contains(MapLocation key)
    {
        // Find head of chain for given key
        int bucketIndex = getBucketIndex(key);
        int hashCode = key.hashCode();
       
        HashNode head = bucketArray.get(bucketIndex);
 
        // Search key in chain
        while (head != null) {
            if (head.key.equals(key) && head.hashCode == hashCode)
                return true;
            head = head.next;
        }
 
        // If key not found
        return false;
    }
 
    // Adds a key value pair to hash
    public void add(MapLocation key, BFSNode value)
    {
        // Find head of chain for given key
        int bucketIndex = getBucketIndex(key);
        int hashCode = key.hashCode();
        HashNode head = bucketArray.get(bucketIndex);
 
        // Check if key is already present
        while (head != null) {
            if (head.key.equals(key) && head.hashCode == hashCode) {
                head.value = value;
                return;
            }
            head = head.next;
        }
 
        // Insert key in chain
        size++;
        head = bucketArray.get(bucketIndex);
        HashNode newNode = new HashNode(key, value, hashCode);
        newNode.next = head;
        bucketArray.set(bucketIndex, newNode);
 
        // If load factor goes beyond threshold, then
        // double hash table size
        if ((1.0 * size) / numBuckets >= 0.7) {
            ArrayList<HashNode> temp = bucketArray;
            bucketArray = new ArrayList<>();
            numBuckets = 2 * numBuckets;
            size = 0;
            for (int i = 0; i < numBuckets; i++)
                bucketArray.add(null);
 
            for (HashNode headNode : temp) {
                while (headNode != null) {
                    add(headNode.key, headNode.value);
                    headNode = headNode.next;
                }
            }
        }
    }
}