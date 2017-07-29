package thut.core.client.render.tabula.model;

import java.io.IOException;

import com.google.common.annotations.Beta;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Beta
public interface IModelParser<T extends IModel> {

    
    
    T decode(ByteBuf buf);

    void encode(ByteBuf buf, T model);

    String getExtension();

    Class<T> getModelClass();

    @SideOnly(Side.CLIENT)
    T parse(String json) throws IOException;

    @SideOnly(Side.CLIENT)
    void render(T model, Entity entity);
}
