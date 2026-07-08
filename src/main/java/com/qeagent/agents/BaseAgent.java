import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.connectors.ai.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.plugin.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.plugin.annotations.KernelFunctionParameter;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.InvocationContext;
import com.microsoft.semantickernel.services.chatcompletion.ToolCallBehavior;

public class OpenRouterAgent {

    // 1. Define a Plugin (Your tool)
    public static class WeatherPlugin {
        @DefineKernelFunction(
            name = "get_weather", 
            description = "Gets the current weather for a given city location."
        )
        public String getWeather(
            @KernelFunctionParameter(
                name = "location", 
                description = "The city name, e.g., London"
            ) String location
        ) {
            if (location.toLowerCase().contains("london")) {
                return "It is 15°C and raining in London.";
            }
            return "It is 22°C and sunny in " + location + ".";
        }
    }

    public static void main(String[] args) {
        // Read your OpenRouter key from environment variables
        String openRouterKey = System.getenv("OPENROUTER_API_KEY");
        
        // 2. Build the Chat Service pointing to OpenRouter
        ChatCompletionService openRouterService = OpenAIChatCompletion.builder()
                .withModelId("google/gemini-2.5-pro") // <-- Any OpenRouter model ID string
                .withApiKey(openRouterKey)
                // Overriding the default OpenAI endpoint to route through OpenRouter
                .withUrl("https://openrouter.ai") 
                .build();

        // 3. Instantiate the Kernel with the OpenRouter service & plugin
        Kernel kernel = Kernel.builder()
                .withAIService(ChatCompletionService.class, openRouterService)
                .withPlugin(new WeatherPlugin(), "WeatherPlugin")
                .build();

        // 4. Force Semantic Kernel to automatically call native Java tools
        InvocationContext invocationContext = InvocationContext.builder()
                .withToolCallBehavior(ToolCallBehavior.allowAllKernelFunctions(true))
                .build();

        // 5. Invoke the system
        String userPrompt = "What is the weather like in London right now?";
        System.out.println("Sending execution request to OpenRouter...");

        FunctionResult result = kernel.invokePromptAsync(userPrompt, invocationContext)
                .block(); // Synchronous block for terminal CLI display

        System.out.println("\n[OpenRouter Java Output]: " + result.getResult());
    }
}
