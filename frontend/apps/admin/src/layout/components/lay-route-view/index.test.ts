import { mount } from "@vue/test-utils";
import { defineComponent, h, nextTick, reactive, shallowRef } from "vue";
import { describe, expect, it } from "vitest";
import LayRouteView from "./index.vue";

const FirstPage = defineComponent({
  template: "<p>first page</p>"
});

const SecondPage = defineComponent({
  template: "<p>second page</p>"
});

const RouteViewHarness = defineComponent({
  setup() {
    return {
      activeComponent: shallowRef(FirstPage),
      activeRoute: reactive({ fullPath: "/first" })
    };
  },
  render() {
    return h(
      LayRouteView,
      {
        component: this.activeComponent,
        route: this.activeRoute as never
      },
      {
        default: ({ component, fullPath }) => h(component, { key: fullPath })
      }
    );
  }
});

describe("layout route view", () => {
  it("renders the latest RouterView component after the route changes", async () => {
    const wrapper = mount(RouteViewHarness);

    expect(wrapper.text()).toBe("first page");

    wrapper.vm.activeComponent = SecondPage;
    wrapper.vm.activeRoute.fullPath = "/second";
    await nextTick();

    expect(wrapper.text()).toBe("second page");
  });
});
