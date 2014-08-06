package org.infinispan.lucene.impl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.jcip.annotations.ThreadSafe;

import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.lucene.ExternalizerIds;

/**
 * Maintains a Set of filenames contained in the index.
 * Does not implement Set for simplicity, and does internal locking to provide
 * a safe Externalizer.
 *
 * @author Sanne Grinovero
 * @since 7.0
 */
@ThreadSafe
public class FileListCacheValue {

   private final HashSet<String> filenames = new HashSet<>();
   private final Lock writeLock;
   private final Lock readLock;

   /**
    * Constructs a new empty set of filenames
    */
   public FileListCacheValue() {
      ReadWriteLock namesLock = new ReentrantReadWriteLock();
      writeLock = namesLock.writeLock();
      readLock = namesLock.readLock();
   }

   /**
    * Initializes a new instance storing the passed values.
    * @param listAll the strings to store.
    */
   public FileListCacheValue(String[] listAll) {
      this();
      Collections.addAll(filenames, listAll);
   }

   /**
    * Removes the filename from the set if it exists
    * @param fileName
    * @return true if the set was mutated
    */
   public boolean remove(String fileName) {
      writeLock.lock();
      try {
         return filenames.remove(fileName);
      }
      finally {
         writeLock.unlock();
      }
   }

   /**
    * Adds the filename from the set if it exists
    * @param fileName
    * @return true if the set was mutated
    */
   public boolean add(String fileName) {
      writeLock.lock();
      try {
         return filenames.add(fileName);
      }
      finally {
         writeLock.unlock();
      }
   }

   public boolean addAndRemove(String toAdd, String toRemove) {
      writeLock.lock();
      try {
         boolean doneAdd = filenames.add(toAdd);
         boolean doneRemove = filenames.remove(toRemove);
         return doneAdd || doneRemove;
      }
      finally {
         writeLock.unlock();
      }
   }

   public String[] toArray() {
      readLock.lock();
      try {
         return filenames.toArray(new String[filenames.size()]);
      }
      finally {
         readLock.unlock();
      }
   }

   public boolean contains(String fileName) {
      readLock.lock();
      try {
         return filenames.contains(fileName);
      }
      finally {
         readLock.unlock();
      }
   }


   public static final class Externalizer extends AbstractExternalizer<FileListCacheValue> {

      @Override
      public void writeObject(final ObjectOutput output, final FileListCacheValue key) throws IOException {
         key.readLock.lock();
         try {
            UnsignedNumeric.writeUnsignedInt(output, key.filenames.size());
            for (String name : key.filenames) {
               output.writeUTF(name);
            }
         }
         finally {
            key.readLock.unlock();
         }
      }

      @Override
      public FileListCacheValue readObject(final ObjectInput input) throws IOException {
         int size = UnsignedNumeric.readUnsignedInt(input);
         String[] names = new String[size];
         for (int i = 0; i < size; i++) {
            names[i] = input.readUTF();
         }
         return new FileListCacheValue(names);
      }

      @Override
      public Integer getId() {
         return ExternalizerIds.FILE_LIST_CACHE_VALUE;
      }

      @Override
      public Set<Class<? extends FileListCacheValue>> getTypeClasses() {
         return Util.<Class<? extends FileListCacheValue>>asSet(FileListCacheValue.class);
      }

   }

}