package main

import (
	"context"
	"encoding/json"
	"log"

	cloudevents "github.com/cloudevents/sdk-go/v2"
	"github.com/rsocket/rsocket-go"
	"github.com/rsocket/rsocket-go/extension"
	"github.com/rsocket/rsocket-go/payload"
	"github.com/rsocket/rsocket-go/rx/flux"
)

func main() {
	var c = make(chan bool)
	// Connect to server
	cli, err := rsocket.Connect().
		SetupPayload(payload.Empty()).
		MetadataMimeType(extension.MessageCompositeMetadata.String()).
		DataMimeType(cloudevents.ApplicationCloudEventsJSON).
		Acceptor(func(ctx context.Context, socket rsocket.RSocket) rsocket.RSocket {
			return rsocket.NewAbstractSocket(
				rsocket.RequestStream(func(input payload.Payload) flux.Flux {
					s := input.DataUTF8()
					m, _ := input.MetadataUTF8()
					log.Println("data:", s, "metadata:", m)
					bytes, _ := json.Marshal(event())
					return flux.Just(payload.New(bytes, nil)).
						DoOnComplete(func() {
							c <- true
						})
				}),
			)
		}).
		Transport(rsocket.TCPClient().SetHostAndPort("127.0.0.1", 9527).Build()).
		Start(context.Background())
	if err != nil {
		panic(err)
	}
	defer func(cli rsocket.Client) {
		err := cli.Close()
		if err != nil {

		}
	}(cli)

	// Send request
	bytes, _ := json.Marshal(event())

	rb, _ := extension.EncodeRouting("cloudevents")

	cmb := extension.CompositeMetadataBuilder{}
	cmb.PushWellKnown(extension.MessageRouting, rb)
	cmb.PushWellKnown(extension.CloudEventsJSON, bytes)

	cm, _ := cmb.Build()

	result, err := cli.RequestResponse(payload.New(bytes, cm)).
		Block(context.Background())
	if err != nil {
		panic(err)
	}

	res := event()
	_ = json.Unmarshal(result.Data(), &res)
	log.Println("response:", res)

	<-c
}

func event() cloudevents.Event {
	event := cloudevents.NewEvent()
	event.SetSource("example/uri")
	event.SetType("example.type")
	_ = event.SetData(cloudevents.ApplicationJSON, map[string]string{"hello": "world"})
	return event
}
