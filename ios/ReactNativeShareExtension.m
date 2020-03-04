#import "ReactNativeShareExtension.h"
#import "React/RCTRootView.h"
#import <MobileCoreServices/MobileCoreServices.h>

NSExtensionContext* extensionContext;

@implementation ReactNativeShareExtension

- (UIView*) shareView
{
    return nil;
}

RCT_EXPORT_MODULE();

- (void)viewDidLoad
{
    [super viewDidLoad];

    extensionContext = self.extensionContext;

    UIView *rootView = [self shareView];
    if (rootView.backgroundColor == nil) {
        rootView.backgroundColor = [[UIColor alloc] initWithRed:1 green:1 blue:1 alpha:0.1];
    }

    self.view = rootView;
}

RCT_EXPORT_METHOD(openURL:(NSString *)url)
{
    UIApplication *application = [UIApplication sharedApplication];
    NSURL *openUrl = [NSURL URLWithString:[url stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
    if (@available(iOS 10.0, *)) {
        [application openURL:openUrl options:@{} completionHandler: nil];
    }
}

RCT_EXPORT_METHOD(close)
{
    [extensionContext completeRequestReturningItems:nil
                                  completionHandler:nil];
    exit(0);
}

RCT_REMAP_METHOD(data, resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    [self extractDataFromContext:extensionContext withCallback:^(NSArray* items, NSException* err) {
        resolve(items);
    }];
}

- (void)extractDataFromContext:(NSExtensionContext *)context withCallback:(void(^)(NSArray *items, NSException *exception))callback
{
    __block NSMutableArray *data = [NSMutableArray new];

    NSExtensionItem *item = [context.inputItems firstObject];
    NSArray *attachments = item.attachments;
    __block NSUInteger index = 0;

    [attachments enumerateObjectsUsingBlock:^(NSItemProvider *provider, NSUInteger idx, BOOL *stop)
    {
        [provider.registeredTypeIdentifiers enumerateObjectsUsingBlock:^(NSString *identifier, NSUInteger idx, BOOL *stop)
        {
            [provider loadItemForTypeIdentifier:identifier options:nil completionHandler:^(id<NSSecureCoding> item, NSError *error)
            {
                index += 1;

                NSString *string;
                NSString *type;

                NSString *className = NSStringFromClass([(NSObject *) item class]);

                NSArray *items = @[@"NSURL", @"NSString", @"UIImage"];
                NSUInteger class = [items indexOfObject:className];
                switch (class) {
                    case 0: {
                        NSURL *url = (NSURL *) item;
                        string = [url absoluteString];
                        type = ([[string pathExtension] isEqualToString:@""]) || [url.scheme containsString:@"http"] ? @"text" : @"media";

                        break;
                    }

                    case 1: {
                        string = (NSString *)item;
                        type = @"text";

                        break;
                    }

                    case 2: {
                        UIImage *sharedImage = (UIImage *)item;
                        NSString *path = [NSTemporaryDirectory() stringByAppendingPathComponent:@"image.png"];
                        [UIImagePNGRepresentation(sharedImage) writeToFile:path atomically:YES];
                        string = [NSString stringWithFormat:@"%@%@", @"file://", path];
                        type = @"media";

                        break;
                    }
                }

                [data addObject:@{
                    @"value": string,
                    @"type": type
                }];

                if (index == [attachments count]) {
                    callback(data, nil);
                }
            }];

            // We'll only use the first provider
            *stop = YES;
        }];
    }];
}

@end
