/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.watabou.glwrap;

import com.badlogic.gdx.Gdx;

import java.nio.FloatBuffer;

public class Attribute {
	private static int browserVertices = -1;

	private int location;
	
	public Attribute( int location ) {
		this.location = location;
	}
	
	public int location() {
		return location;
	}
	
	public void enable() {
		Gdx.gl.glEnableVertexAttribArray( location );
	}
	
	public void disable() {
		Gdx.gl.glDisableVertexAttribArray( location );
	}
	
	public void vertexPointer( int size, int stride, FloatBuffer ptr ) {
		if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.WebGL) {
			// WebGL forbids native client-memory pointers. Upload a complete view and retain its attribute offset.
			int offset = ptr.position();
			FloatBuffer upload = ptr.duplicate();
			((java.nio.Buffer)upload).position(0);
			if (browserVertices == -1) browserVertices = Gdx.gl.glGenBuffer();
			Gdx.gl.glBindBuffer(Gdx.gl.GL_ARRAY_BUFFER, browserVertices);
			Gdx.gl.glBufferData(Gdx.gl.GL_ARRAY_BUFFER, upload.limit()*4, upload.slice(), Gdx.gl.GL_DYNAMIC_DRAW);
			vertexBuffer(size, stride, offset);
			return;
		}
		Gdx.gl.glVertexAttribPointer( location, size, Gdx.gl.GL_FLOAT, false, stride * 4, ptr );
	}

	public void vertexBuffer( int size, int stride, int offset) {
		Gdx.gl.glVertexAttribPointer(location, size, Gdx.gl.GL_FLOAT, false, stride * 4, offset * 4);
	}
}
