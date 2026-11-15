/*
 * Copyright 2012, Google LLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google LLC nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.tools.smali.dexlib2.dexbacked;

import com.android.tools.smali.dexlib2.HiddenApiRestriction;
import com.android.tools.smali.dexlib2.base.reference.BaseMethodReference;
import com.android.tools.smali.dexlib2.dexbacked.raw.MethodIdItem;
import com.android.tools.smali.dexlib2.dexbacked.raw.ProtoIdItem;
import com.android.tools.smali.dexlib2.dexbacked.raw.TypeListItem;
import com.android.tools.smali.dexlib2.dexbacked.reference.DexBackedMethodReference;
import com.android.tools.smali.dexlib2.dexbacked.util.AnnotationsDirectory;
import com.android.tools.smali.dexlib2.dexbacked.util.AnnotationsDirectory.AnnotationIterator;
import com.android.tools.smali.dexlib2.dexbacked.util.FixedSizeList;
import com.android.tools.smali.dexlib2.dexbacked.util.ParameterIterator;
import com.android.tools.smali.dexlib2.iface.Annotation;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodParameter;
import com.android.tools.smali.util.AbstractForwardSequentialList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DexBackedMethod extends BaseMethodReference implements Method {
    @Nonnull public final DexBackedDexFile dexFile;
    @Nonnull public final DexBackedClassDef classDef;

    public final int accessFlags;

    private final int codeOffset;
    /** The annotation offsets are looked up when first needed if they are {@link #UNKNOWN_OFFSET}. */
    private static final int UNKNOWN_OFFSET = -1;
    private int parameterAnnotationSetListOffset;
    private int methodAnnotationSetOffset;
    private final int hiddenApiRestrictions;

    public final int methodIndex;
    private final int startOffset;

    private int methodIdItemOffset;
    private int protoIdItemOffset;
    private int parametersOffset = -1;
    private int cachedHashCode;

    /**
     * Creates a method that looks up its annotations in the annotations directory of the class when they are
     * first needed, instead of seeking annotation iterators for each method while iterating.
     */
    public DexBackedMethod(@Nonnull DexBackedDexFile dexFile,
                           @Nonnull DexReader<? extends DexBuffer> reader,
                           @Nonnull DexBackedClassDef classDef,
                           int previousMethodIndex,
                           int hiddenApiRestrictions) {
        this.dexFile = dexFile;
        this.classDef = classDef;
        startOffset = reader.getOffset();

        // large values may be used for the index delta, which cause the cumulative index to overflow upon
        // addition, effectively allowing out of order entries.
        int methodIndexDiff = reader.readLargeUleb128();
        this.methodIndex = methodIndexDiff + previousMethodIndex;
        this.accessFlags = reader.readSmallUleb128();
        this.codeOffset = reader.readSmallUleb128();
        this.hiddenApiRestrictions = hiddenApiRestrictions;

        this.methodAnnotationSetOffset = UNKNOWN_OFFSET;
        this.parameterAnnotationSetListOffset = UNKNOWN_OFFSET;
    }

    public DexBackedMethod(@Nonnull DexBackedDexFile dexFile,
                           @Nonnull DexReader<? extends DexBuffer> reader,
                           @Nonnull DexBackedClassDef classDef,
                           int previousMethodIndex,
                           @Nonnull AnnotationIterator methodAnnotationIterator,
                           @Nonnull AnnotationIterator paramaterAnnotationIterator,
                           int hiddenApiRestrictions) {
        this.dexFile = dexFile;
        this.classDef = classDef;
        startOffset = reader.getOffset();

        // large values may be used for the index delta, which cause the cumulative index to overflow upon
        // addition, effectively allowing out of order entries.
        int methodIndexDiff = reader.readLargeUleb128();
        this.methodIndex = methodIndexDiff + previousMethodIndex;
        this.accessFlags = reader.readSmallUleb128();
        this.codeOffset = reader.readSmallUleb128();
        this.hiddenApiRestrictions = hiddenApiRestrictions;

        this.methodAnnotationSetOffset = methodAnnotationIterator.seekTo(methodIndex);
        this.parameterAnnotationSetListOffset = paramaterAnnotationIterator.seekTo(methodIndex);
    }

    public int getMethodIndex() { return methodIndex; }
    @Nonnull @Override public String getDefiningClass() { return classDef.getType(); }
    @Override public int getAccessFlags() { return accessFlags; }

    @Nonnull
    @Override
    public String getName() {
        return dexFile.getStringSection().get(
                dexFile.getBuffer().readSmallUint(getMethodIdItemOffset() + MethodIdItem.NAME_OFFSET));
    }

    @Nonnull
    @Override
    public String getReturnType() {
        return dexFile.getTypeSection().get(
                dexFile.getBuffer().readSmallUint(getProtoIdItemOffset() + ProtoIdItem.RETURN_TYPE_OFFSET));
    }

    @Nonnull
    @Override
    public List<? extends MethodParameter> getParameters() {
        int parametersOffset = getParametersOffset();
        if (parametersOffset > 0) {
            final List<String> parameterTypes = getParameterTypes();

            return new AbstractForwardSequentialList<MethodParameter>() {
                @Nonnull @Override public Iterator<MethodParameter> iterator() {
                    return new ParameterIterator(parameterTypes,
                            getParameterAnnotations(),
                            getParameterNames());
                }

                @Override public int size() {
                    return parameterTypes.size();
                }
            };
        }
        return Collections.emptyList();
    }

    @Nonnull
    public List<? extends Set<? extends DexBackedAnnotation>> getParameterAnnotations() {
        return AnnotationsDirectory.getParameterAnnotations(dexFile, getParameterAnnotationSetListOffset());
    }

    @Nonnull
    public Iterator<String> getParameterNames() {
        DexBackedMethodImplementation methodImpl = getImplementation();
        if (methodImpl != null) {
            return methodImpl.getParameterNames(null);
        }
        return Collections.emptyIterator();
    }

    @Nonnull
    @Override
    public List<String> getParameterTypes() {
        final int parametersOffset = getParametersOffset();
        if (parametersOffset > 0) {
            final int parameterCount = dexFile.getDataBuffer().readSmallUint(parametersOffset + TypeListItem.SIZE_OFFSET);
            final int paramListStart = parametersOffset + TypeListItem.LIST_OFFSET;
            return new FixedSizeList<String>() {
                @Nonnull
                @Override
                public String readItem(final int index) {
                    return dexFile.getTypeSection().get(dexFile.getDataBuffer().readUshort(paramListStart + 2*index));
                }
                @Override public int size() { return parameterCount; }
            };
        }
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Set<? extends Annotation> getAnnotations() {
        return AnnotationsDirectory.getAnnotations(dexFile, getMethodAnnotationSetOffset());
    }

    @Nonnull
    @Override
    public Set<HiddenApiRestriction> getHiddenApiRestrictions() {
        if (hiddenApiRestrictions == DexBackedClassDef.NO_HIDDEN_API_RESTRICTIONS) {
            return Collections.emptySet();
        } else {
            return EnumSet.copyOf(HiddenApiRestriction.getAllFlags(hiddenApiRestrictions));
        }
    }

    @Nullable
    @Override
    public DexBackedMethodImplementation getImplementation() {
        if (codeOffset > 0) {
            return dexFile.createMethodImplementation(dexFile, this, codeOffset);
        }
        return null;
    }

    private int getMethodAnnotationSetOffset() {
        int offset = methodAnnotationSetOffset;
        if (offset == UNKNOWN_OFFSET) {
            offset = classDef.getAnnotationsDirectory().getMethodAnnotationSetOffset(methodIndex);
            methodAnnotationSetOffset = offset;
        }
        return offset;
    }

    private int getParameterAnnotationSetListOffset() {
        int offset = parameterAnnotationSetListOffset;
        if (offset == UNKNOWN_OFFSET) {
            offset = classDef.getAnnotationsDirectory().getParameterAnnotationSetListOffset(methodIndex);
            parameterAnnotationSetListOffset = offset;
        }
        return offset;
    }

    private int getMethodIdItemOffset() {
        if (methodIdItemOffset == 0) {
            methodIdItemOffset = dexFile.getMethodSection().getOffset(methodIndex);
        }
        return methodIdItemOffset;
    }

    private int getProtoIdItemOffset() {
        if (protoIdItemOffset == 0) {
            int protoIndex = dexFile.getBuffer().readUshort(getMethodIdItemOffset() + MethodIdItem.PROTO_OFFSET);
            protoIdItemOffset = dexFile.getProtoSection().getOffset(protoIndex);
        }
        return protoIdItemOffset;
    }

    private int getParametersOffset() {
        if (parametersOffset == -1) {
            parametersOffset = dexFile.getBuffer().readSmallUint(
                    getProtoIdItemOffset() + ProtoIdItem.PARAMETERS_OFFSET);
        }
        return parametersOffset;
    }

    /**
     * Skips the reader over the specified number of encoded_method structures
     *
     * @param reader The reader to skip
     * @param count The number of encoded_method structures to skip over
     */
    public static void skipMethods(@Nonnull DexReader<? extends DexBuffer> reader, int count) {
        for (int i=0; i<count; i++) {
            reader.skipUleb128();
            reader.skipUleb128();
            reader.skipUleb128();
        }
    }

    /**
     * Calculate and return the private size of a method definition.
     *
     * Calculated as: method_idx_diff + access_flags + code_off +
     * implementation size + reference size
     *
     * @return size in bytes
     */
    public int getSize() {
        int size = 0;

        DexReader<? extends DexBuffer> reader = dexFile.getDataBuffer().readerAt(startOffset);
        reader.readLargeUleb128(); //method_idx_diff
        reader.readSmallUleb128(); //access_flags
        reader.readSmallUleb128(); //code_off
        size += reader.getOffset() - startOffset;

        DexBackedMethodImplementation impl = getImplementation();
        if (impl != null) {
            size += impl.getSize();
        }

        DexBackedMethodReference methodRef = new DexBackedMethodReference(dexFile, methodIndex);
        size += methodRef.getSize();

        return size;
    }

    @Override
    public int hashCode() {
        // The dex file can't change, so the hash code is calculated only once.
        int hashCode = cachedHashCode;
        if (hashCode == 0) {
            hashCode = super.hashCode();
            cachedHashCode = hashCode;
        }
        return hashCode;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o instanceof DexBackedMethod) {
            DexBackedMethod other = (DexBackedMethod) o;
            if (other.dexFile == dexFile && other.classDef == classDef) {
                if (other.methodIndex == methodIndex) {
                    return true;
                }
                int offset = getMethodIdItemOffset();
                int otherOffset = other.getMethodIdItemOffset();
                DexBuffer buffer = dexFile.getBuffer();
                return buffer.readSmallUint(offset + MethodIdItem.NAME_OFFSET) ==
                        buffer.readSmallUint(otherOffset + MethodIdItem.NAME_OFFSET) &&
                        buffer.readUshort(offset + MethodIdItem.PROTO_OFFSET) ==
                        buffer.readUshort(otherOffset + MethodIdItem.PROTO_OFFSET);
            }
        }
        return super.equals(o);
    }
}
