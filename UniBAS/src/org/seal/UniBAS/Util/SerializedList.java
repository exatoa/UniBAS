package org.seal.UniBAS.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.RandomAccess;

public class SerializedList<E> extends AbstractList<E> implements List<E>, RandomAccess, java.io.Serializable {
	/**
	 * serialVersionUID 자동할당됨.
	 */
	private static final long serialVersionUID = -7599769021856200140L;
	// private static final long serialVersionUID = 8683452581122892189L;	//ArrayList의 키값.
	
	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;


	private transient Object[] elementData;		//리스트 객체
	private int size;							//리스트의 크기


	
	private File file;
	
	
	/**
	 * Returns the number of elements in this list.
	 * @return the number of elements in this list 
	 */
	public int size() {
		return size;
	}


	/**
	 * Returns true if this list contains no elements.
	 * @return true if this list contains no elements.
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	//##########################################################################
	// Behaviors
	//##########################################################################
	

	/**
	 * Constructs an empty list with the specified initial capacity.
	 * @param initialCapacity : the initial capacity of the list
	 * @Throws java.lang.IllegalArgumentException if the specified initial capacity is negative
	 */
	public SerializedList(int initialCapacity, String _path) {
		super();
		if (initialCapacity < 0)
			throw new IllegalArgumentException("Illegal Capacity: "	+ initialCapacity);
		this.elementData = new Object[initialCapacity];
			
		try {
			file = new File(_path);
			File parent = file.getParentFile();

			if(!parent.exists() && !parent.mkdirs()){
			    throw new IllegalStateException("Couldn't create dir: " + parent);
			}
			
			if(file.exists()==false)
			{
				writeObject();
			}			
			readObject();
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Constructs an empty list with an initial capacity of ten. 
	 * @Throws java.lang.IllegalArgumentException if the specified initial capacity is negative
	 */
	public SerializedList(String _path) {
		this(10, _path);
	}

	
	/**
	 * Collection 복사생성자
	 * @param c the collection whose elements are to be placed into this list
	 * @throws java.lang.NullPointerException if the specified collection is null
	 */
	public SerializedList(Collection<? extends E> c) {
		elementData = c.toArray();
		size = elementData.length;
		// c.toArray might (incorrectly) not return Object[] (see 6260652)
		if (elementData.getClass() != Object[].class)
			elementData = Arrays.copyOf(elementData, size, Object[].class);
	}
	
	/**
	 * 파일을 정리하고 삭제하도록 하는 명령.
	 * 리스트가 없어야함.
	 */
	public void close() {
		if(file.exists()==true)
		{
			ObjectInputStream s;
			try {
				s = new ObjectInputStream(new FileInputStream(file));
				@SuppressWarnings("unused")
				int Capacity = s.readInt();
				int size = s.readInt();
				if (size==0)
					file.delete();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}

	/**
	 * 배열크기에 맞게 Capacity 맞춤.
	 */
	public void trimToSize() {
		modCount++;
		int oldCapacity = elementData.length;
		if (size < oldCapacity) {
			elementData = Arrays.copyOf(elementData, size);
		}
	}


	/**
	 * Increases the capacity of this ArrayList instance, if necessary, to
	 * ensure that it can hold at least the number of elements specified by the
	 * minimum capacity argument.
	 * 
	 * @param minCapacity the desired minimum capacity
	 */
	public void ensureCapacity(int minCapacity) {
		modCount++;
		int oldCapacity = elementData.length;
		
		if (minCapacity > oldCapacity) {
			int newCapacity = (oldCapacity * 3) / 2 + 1;
			if (newCapacity < minCapacity)
				newCapacity = minCapacity;
			// minCapacity is usually close to size, so this is a win:
			elementData = Arrays.copyOf(elementData, newCapacity);
		}
	}

	/**
	 * Returns true if this list contains the specified element. More formally,
	 * such that (o==null ? e==null : o.equals(e)).
	 * 
	 * @param o element whose presence in this list is to be tested
	 * @return true if this list contains the specified element
	 */
	public boolean contains(Object o) {
		return indexOf(o) >= 0;
	}


	/**
	 * Returns the index of the first occurrence of the specified element in
	 * this list, or -1 if this list does not contain the element. More
	 * formally, returns the lowest index i such that (o==null ? get(i)==null :
	 * o.equals(get(i))), or -1 if there is no such index.
	 * 
	 */
	public int indexOf(Object o) {
		if (o == null) {
			for (int i = 0; i < size; i++)
				if (elementData[i] == null)
					return i;
		} else {
			for (int i = 0; i < size; i++)
				if (o.equals(elementData[i]))
					return i;
		}
		return -1;
	}

	/**
	 * Returns the index of the last occurrence of the specified element in this
	 * list, or -1 if this list does not contain the element. More formally,
	 * returns the highest index i such that (o==null ? get(i)==null :
	 * o.equals(get(i))), or -1 if there is no such index. 
	 */
	public int lastIndexOf(Object o) {
		if (o == null) {
			for (int i = size - 1; i >= 0; i--)
				if (elementData[i] == null)
					return i;
		} else {
			for (int i = size - 1; i >= 0; i--)
				if (o.equals(elementData[i]))
					return i;
		}
		return -1;
	}

	/**
	 * Returns an array containing all of the elements in this list in proper
	 * sequence (from first to last element).
	 * The returned array will be "safe" in that no references to it are
	 * maintained by this list. (In other words, this method must allocate a new
	 * array). The caller is thus free to modify the returned array.
	 * This method acts as bridge between array-based and collection-based APIs.
	 * @return an array containing all of the elements in this list in proper sequence
	 */

	public Object[] toArray() {
		return Arrays.copyOf(elementData, size);
	}

	/**
	 * Returns an array containing all of the elements in this list in proper
	 * sequence (from first to last element); the runtime type of the returned
	 * array is that of the specified array. If the list fits in the specified
	 * array, it is returned therein. Otherwise, a new array is allocated with
	 * the runtime type of the specified array and the size of this list.
	 * If the list fits in the specified array with room to spare (i.e., the
	 * array has more elements than the list), the element in the array
	 * immediately following the end of the collection is set to null. (This is
	 * useful in determining the length of the list only if the caller knows
	 * that the list does not contain any null elements.)
	 * @param a the array into which the elements of the list are to be stored, if it
	 * is big enough; otherwise, a new array of the same runtime type is
	 * allocated for this purpose.
	 * @return an array containing the elements of the list
	 * @throws
	 * java.lang.ArrayStoreException if the runtime type of the specified array
	 * is not a supertype of the runtime type of every element in this list
	 * java.lang.NullPointerException if the specified array is null
	 */
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		if (a.length < size)
			// Make a new array of a's runtime type, but my contents:
			return (T[]) Arrays.copyOf(elementData, size, a.getClass());
		System.arraycopy(elementData, 0, a, 0, size);
		if (a.length > size)
			a[size] = null;
		return a;
	}

	/**
	 * Positional Access Operations
	 * @param index
	 * @return
	 */
	@SuppressWarnings("unchecked")
	E elementData(int index) {
		return (E) elementData[index];
	}

	/**
	 * Returns the element at the specified position in this list.
	 * @param index index of the element to return
	 * @return the element at the specified position in this list
	 * @throws
	 * java.lang.IndexOutOfBoundsException
	 */
	public E get(int index) {
		rangeCheck(index);
		return elementData(index);
	}

	/**
	 * Replaces the element at the specified position in this list with the
	 * specified element.
	 * @param index index of the element to replace
	 * @param element element to be stored at the specified position
	 * @return the element previously at the specified position
	 * @throws
	 * java.lang.IndexOutOfBoundsException
 	*/
	public E set(int index, E element) {
		rangeCheck(index);

		E oldValue = elementData(index);
		elementData[index] = element;
		return oldValue;
	}

	/**
	 * Appends the specified element to the end of this list.
	 * @param e element to be appended to this list
	 * @return true (as specified by Collection.add(java.lang.Object))
 	*/
	public boolean add(E e) {
		ensureCapacity(size + 1); // Increments modCount!!
		elementData[size++] = e;

		try {
			writeObject();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return true;
	}

	/**
	 * Inserts the specified element at the specified position in this list.
	 * Shifts the element currently at that position (if any) and any subsequent
	 * elements to the right (adds one to their indices).
	 * @param index index at which the specified element is to be inserted
	 * element element to be inserted
	 * @throws
	 * java.lang.IndexOutOfBoundsException
 	*/
	public void add(int index, E element) {
		rangeCheckForAdd(index);

		ensureCapacity(size + 1); // Increments modCount!!
		System.arraycopy(elementData, index, elementData, index + 1, size - index);
		elementData[index] = element;
		size++;
		
		try {
			writeObject();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	/**
	 * Append the specified elements.
	 * @param c 입력할 엘리먼트들의 리스트.
	 * @return 
	 * @throws
	 * java.lang.IndexOutOfBoundsException
 	*/
	public boolean addAll(Collection<? extends E> c) {
		
		Object[] a = c.toArray();
		int cSize = a.length;
		ensureCapacity(size + cSize); // Increments modCount!!
		System.arraycopy(a, 0, elementData, size, cSize);
		size += cSize;
		boolean flag = cSize!=0;
		
		if(flag==true){
			try {
				writeObject();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		return flag;
	}
	
	/**
	 * Inserts the specified elements at the specified position in this list.
	 * Shifts the element currently at that position (if any) and any subsequent
	 * elements to the right (adds one to their indices).
	 * @param c 입력할 엘리먼트들의 리스트.
	 * @return 
	 * @throws
	 * java.lang.IndexOutOfBoundsException
 	*/
	public boolean addAll(int index, Collection<? extends E> c) {
		rangeCheckForAdd(index);

		Object[] a = c.toArray();
		int cSize = a.length;
		ensureCapacity(size + cSize); // Increments modCount!!
		System.arraycopy(elementData, index, elementData, index + cSize, size - index);
		System.arraycopy(c, 0, elementData, index, cSize);
		size += cSize;		
		boolean flag = cSize!=0;
		
		if(flag==true){
			try {
				writeObject();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		return flag;
	}

	/**
	 * Removes the element at the specified position in this list. Shifts any
	 * subsequent elements to the left (subtracts one from their indices).
	 * @param index the index of the element to be removed
	 * @return the element that was removed from the list
	 * @throws
	 * java.lang.IndexOutOfBoundsException
 	*/
	public E remove(int index) {
		rangeCheck(index);

		modCount++;
		E oldValue = elementData(index);

		int numMoved = size - index - 1;
		if (numMoved > 0)
			System.arraycopy(elementData, index + 1, elementData, index,
					numMoved);
		elementData[--size] = null; // Let gc do its work

		return oldValue;
	}

	/**
	 * Removes the first occurrence of the specified element from this list, if
	 * it is present. If the list does not contain the element, it is unchanged.
	 * More formally, removes the element with the lowest index i such that
	 * (o==null ? get(i)==null : o.equals(get(i))) (if such an element exists).
	 * Returns true if this list contained the specified element (or
	 * equivalently, if this list changed as a result of the call).
	 * @param o element to be removed from this list, if present
	 * @return true if this list contained the specified element
 	*/
	public boolean remove(Object o) {
		if (o == null) {
			for (int index = 0; index < size; index++)
				if (elementData[index] == null) {
					fastRemove(index);
					return true;
				}
		} else {
			for (int index = 0; index < size; index++)
				if (o.equals(elementData[index])) {
					fastRemove(index);
					return true;
				}
		}
		return false;
	}

	/*
	 * Private remove method that skips bounds checking and does not return the
	 * value removed.
	 */
	private void fastRemove(int index) {
		modCount++;
		int numMoved = size - index - 1;
		if (numMoved > 0)
			System.arraycopy(elementData, index + 1, elementData, index,
					numMoved);
		elementData[--size] = null; // Let gc do its work
	}

	/**
	 * Removes all of the elements from this list. The list will be empty after
	 * this call returns.
 	 */
	public void clear() {
		modCount++;

		// Let gc do its work
		for (int i = 0; i < size; i++)
			elementData[i] = null;

		size = 0;
		
		try {
			writeObject();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	

	/**
	 * Removes from this list all of the elements whose index is between
	 * fromIndex, inclusive, and toIndex, exclusive. Shifts any succeeding
	 * elements to the left (reduces their index). This call shortens the list
	 * by (toIndex - fromIndex) elements. (If toIndex==fromIndex, this operation
	 * has no effect.)
	 * @throws
	 * java.lang.IndexOutOfBoundsException if fromIndex or toIndex is out of
	 * range (fromIndex < 0 || fromIndex >= size() || toIndex > size() ||
	 * toIndex < fromIndex)
 	 */
	protected void removeRange(int fromIndex, int toIndex) {
		modCount++;
		int numMoved = size - toIndex;
		System.arraycopy(elementData, toIndex, elementData, fromIndex, numMoved);

		// Let gc do its work
		int newSize = size - (toIndex - fromIndex);
		while (size != newSize)
			elementData[--size] = null;
		
		try {
			writeObject();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Checks if the given index is in range. If not, throws an appropriate
	 * runtime exception. This method does *not* check if the index is negative:
	 * It is always used immediately prior to an array access, which throws an
	 * ArrayIndexOutOfBoundsException if index is negative.
 	 */
	private void rangeCheck(int index) {
		if (index >= size)
			throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
	}

	/**
	 * A version of rangeCheck used by add and addAll.
 	 */
	private void rangeCheckForAdd(int index) {
		if (index > size || index < 0)
			throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
	}

	/**
	 * Constructs an IndexOutOfBoundsException detail message. Of the many
	 * possible refactorings of the error handling code, this "outlining"
	 * performs best with both server and client VMs.
 	 */
	private String outOfBoundsMsg(int index) {
		return "Index: " + index + ", Size: " + size;
	}

	/**
	 * Removes from this list all of its elements that are contained in the
	 * specified collection.
	 * @param c collection containing elements to be removed from this list
	 * @return true if this list changed as a result of the call
	 * @throws
	 * java.lang.ClassCastException if the class of an element of this list is
	 * incompatible with the specified collection (optional)
	 * java.lang.NullPointerException if this list contains a null element and
	 * the specified collection does not permit null elements (optional), or if
	 * the specified collection is null
	 * @see Collection.contains(java.lang.Object)
 	 */
	public boolean removeAll(Collection<?> c) {
		return batchRemove(c, false);
	}

	/**
	 * Retains only the elements in this list that are contained in the
	 * specified collection. In other words, removes from this list all of its
	 * elements that are not contained in the specified collection.
	 * @param c collection containing elements to be retained in this list
	 * @returns true if this list changed as a result of the call
	 * @throws
	 * java.lang.ClassCastException if the class of an element of this list is
	 * incompatible with the specified collection (optional)
	 * java.lang.NullPointerException if this list contains a null element and
	 * the specified collection does not permit null elements (optional), or if
	 * the specified collection is null
	 * @see Collection.contains(java.lang.Object)
 	 */
	public boolean retainAll(Collection<?> c) {
		return batchRemove(c, true);
	}

	private boolean batchRemove(Collection<?> c, boolean complement) {
		final Object[] elementData = this.elementData;
		int r = 0, w = 0;
		boolean modified = false;
		try {
			for (; r < size; r++)
				if (c.contains(elementData[r]) == complement)
					elementData[w++] = elementData[r];
		} finally {
			// Preserve behavioral compatibility with AbstractCollection,
			// even if c.contains() throws.
			if (r != size) {
				System.arraycopy(elementData, r, elementData, w, size - r);
				w += size - r;
			}
			if (w != size) {
				for (int i = w; i < size; i++)
					elementData[i] = null;
				modCount += size - w;
				size = w;
				modified = true;
			}
		}
		
		try {
			writeObject();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return modified;
	}

	/**
	 * Save the state of the ArrayList instance to a stream (that is, serialize it).
	 * SerialData:
	 * The length of the array backing the ArrayList instance is emitted (int),
	 * followed by all of its elements (each an Object) in the proper order.
 	 */
	@SuppressWarnings("resource")
	private void writeObject()	throws java.io.IOException {
		// Write out element count, and any hidden stuff
		int expectedModCount = modCount;
		//s.defaultWriteObject();
		ObjectOutputStream s = new ObjectOutputStream(new FileOutputStream(file));

		// Write out array length
		s.writeInt(elementData.length);
		s.writeInt(size);

		// Write out all elements in the proper order.
		for (int i = 0; i < size; i++)
			s.writeObject(elementData[i]);

		if (modCount != expectedModCount) {
			throw new ConcurrentModificationException();
		}
		s.close();
	}

	/**
	 * Reconstitute the ArrayList instance from a stream (that is, deserialize it).
 	 */
	private void readObject() throws java.io.IOException, ClassNotFoundException {
		// Read in size, and any hidden stuff
		//s.defaultReadObject();
		ObjectInputStream s = new ObjectInputStream(new FileInputStream(file));

		// Read in array length and allocate array
		int arrayLength = s.readInt();
		size =s.readInt(); 

		Object[] a = elementData = new Object[arrayLength];

		// Read in all elements in the proper order.
		ensureCapacity(arrayLength);
		for (int i = 0; i < size; i++)
			a[i] = s.readObject();
		
		s.close();
	}


}