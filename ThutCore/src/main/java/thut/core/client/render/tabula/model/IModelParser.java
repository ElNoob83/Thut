package thut.core.client.render.tabula.model;

import java.io.IOException;

import com.google.common.annotations.Beta;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@Beta
public interface IModelParser<T extends IModel> {

    
    
    T decode(ByteBuf buf);

    void encode(ByteBuf buf, T model);

    String getExtension();

    Class<T> getModelClass();

    @OnlyIn(Dist.CLIENT)
    T parse(String json) throws IOException;

    @OnlyIn(Dist.CLIENT)
    void render(T model, Entity entity);
}
