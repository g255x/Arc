package dev.luminous.api.events.eventbus;

public interface IEventBus {
   void registerLambdaFactory(LambdaListener.Factory var1);

   Object post(Object var1);

   ICancellable post(ICancellable var1);

   void subscribe(Object var1);

   void subscribe(Class var1);

   void subscribe(IListener var1);

   void unsubscribe(Object var1);

   void unsubscribe(Class var1);

   void unsubscribe(IListener var1);
}
